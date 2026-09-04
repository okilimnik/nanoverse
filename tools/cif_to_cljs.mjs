// Turn PDB Chemical Component Dictionary entries into the Squint data literal
// that src/nanoverse/aminoacid/residues.cljs holds.
//
// Twenty-one residues is too much geometry to transcribe by hand without
// getting a digit wrong somewhere, and a hand-typo in a coordinate is exactly
// the kind of invented chemistry this project is not allowed to have. So the
// literal is GENERATED, the .cif files it is generated from are committed
// verbatim next to it, and this script is committed too -- the provenance
// chain is re-runnable rather than asserted.
//
//   node tools/cif_to_cljs.mjs > src/nanoverse/aminoacid/residues.cljs
//
// What it reads:
//   _chem_comp_atom  -> pdbx_model_Cartn_{x,y,z}_ideal, and type_symbol, so the
//                       element is carried as data rather than guessed from the
//                       atom name
//   _chem_comp_bond  -> value_order, plus pdbx_aromatic_flag. An aromatic bond
//                       is emitted as order 1.5, NOT as the SING/DOUB the file
//                       says: a CIF has to pick one Kekule structure for a ring
//                       whose electrons are delocalised, and the deck already
//                       draws a delocalised bond as one-and-a-half.

import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");

// Teaching order, not alphabetical: the order the four slides walk them in.
const RESIDUES = [
  // charged
  "ASP", "GLU", "LYS", "ARG", "HIS",
  // polar, uncharged
  "SER", "THR", "ASN", "GLN",
  // special cases
  "GLY", "PRO", "CYS", "SEC",
  // nonpolar, hydrophobic
  "ALA", "VAL", "LEU", "ILE", "MET", "PHE", "TYR", "TRP",
];

const BOND_ORDER = { SING: 1, DOUB: 2, TRIP: 3 };

/** Every row of the `loop_` whose header names start with `prefix`, keyed by column name. */
function loopRows(text, prefix) {
  const lines = text.split("\n");
  const names = [];
  const rows = [];
  let inHeader = false;
  let inBody = false;
  for (const line of lines) {
    const t = line.trim();
    if (t.startsWith(prefix)) {
      if (inBody) break; // a second loop with the same prefix: not expected
      names.push(t.replace(prefix, ""));
      inHeader = true;
      continue;
    }
    if (inHeader && !inBody) {
      if (t === "" || t === "#" || t === "loop_") { names.length = 0; inHeader = false; continue; }
      inBody = true;
    }
    if (inBody) {
      if (t === "" || t === "#" || t === "loop_") break;
      // CCD rows are whitespace-separated with quoted values only for names,
      // which are not columns we read.
      const cells = t.match(/"[^"]*"|\S+/g) ?? [];
      const row = {};
      names.forEach((n, i) => { row[n] = (cells[i] ?? "").replace(/^"|"$/g, ""); });
      rows.push(row);
    }
  }
  return rows;
}

function scalar(text, key) {
  const m = text.match(new RegExp(`^${key.replace(/\./g, "\\.")}\\s+(.+)$`, "m"));
  return m ? m[1].trim().replace(/^"|"$/g, "").replace(/^'|'$/g, "") : "";
}

const pad = (s, n) => String(s).padEnd(n);
const num = (s) => {
  const v = Number(s);
  if (!Number.isFinite(v)) throw new Error(`non-numeric coordinate ${s}`);
  return v.toFixed(3).padStart(8);
};

const out = [];
out.push(`(ns nanoverse.aminoacid.residues)

;; GENERATED FILE -- do not edit by hand.
;;
;;   node tools/cif_to_cljs.mjs > src/nanoverse/aminoacid/residues.cljs
;;
;; The twenty standard amino acids plus selenocysteine, straight out of the PDB
;; Chemical Component Dictionary: ideal coordinates in angstroms, elements, and
;; bond orders, all read off the .cif files kept verbatim in structures/.
;;
;; Read this as data, not as code. What each side chain is ALLOWED TO DO --
;; which atom accepts, which hydrogen donates, which surface is greasy, what
;; the pKa is -- is declared next door in nanoverse.aminoacid.geometry, by
;; hand, because those are chemical claims and they should be somewhere a
;; person can check them.
;;
;; Two things here are already an interpretation and are worth knowing about:
;;
;;   * An aromatic bond is emitted as :order 1.5. The file writes each ring
;;     bond as SING or DOUB because a CIF has to commit to one Kekule
;;     structure; the ring's electrons are not actually arranged that way, and
;;     the deck already draws a delocalised bond as one-and-a-half.
;;
;;   * SOME OF THESE ENTRIES ARE ALREADY IONS. The CCD deposits arginine,
;;     lysine and histidine in their protonated, cationic forms (formal charge
;;     +1) and aspartate/glutamate in their neutral acid forms. That is noted
;;     per residue below, and it decides which direction each slide has to
;;     construct from -- removing an atom is exact, adding one is not.
`);

const charges = [];

for (const code of RESIDUES) {
  const text = readFileSync(join(ROOT, "structures", `${code}.cif`), "utf8");
  const name = scalar(text, "_chem_comp.name").toLowerCase();
  const formula = scalar(text, "_chem_comp.formula");
  const formal = scalar(text, "_chem_comp.pdbx_formal_charge");
  charges.push([code, Number(formal)]);

  const atoms = loopRows(text, "_chem_comp_atom.");
  const bonds = loopRows(text, "_chem_comp_bond.");
  if (!atoms.length || !bonds.length) throw new Error(`${code}: empty loop`);

  const w = Math.max(...atoms.map((a) => a.atom_id.length));
  const ew = Math.max(...atoms.map((a) => a.type_symbol.length));

  out.push(`;; ---------------------------------------------------------------------`);
  out.push(`;; ${code} -- ${name}, ${formula}, formal charge ${formal}`);
  out.push(`;; ---------------------------------------------------------------------`);
  out.push(`(def ${code}`);
  out.push(`  {:code "${code}"`);
  out.push(`   :formula "${formula}"`);
  out.push(`   :formal-charge ${Number(formal)}`);
  out.push(`   :atoms`);
  const atomLines = atoms.map((a) => {
    const q = Number(a.charge);
    const note = q ? `  ; formal charge ${q > 0 ? "+" : ""}${q}` : "";
    return `    :${pad(a.atom_id, w)} {:e :${pad(a.type_symbol, ew)} :x ${num(a.pdbx_model_Cartn_x_ideal)} :y ${num(a.pdbx_model_Cartn_y_ideal)} :z ${num(a.pdbx_model_Cartn_z_ideal)}}${note}`;
  });
  out.push(`   {${atomLines.join("\n").replace(/^ {4}/, "")}}`);
  out.push(`   :bonds`);
  const bondLines = bonds.map((b) => {
    const aromatic = b.pdbx_aromatic_flag === "Y";
    const order = aromatic ? "1.5" : String(BOND_ORDER[b.value_order] ?? 1);
    const note = aromatic ? `  ; aromatic: file says ${b.value_order}` : "";
    return `    {:a :${pad(b.atom_id_1, w)} :b :${pad(b.atom_id_2, w)} :order ${pad(order, 3)}}${note}`;
  });
  out.push(`   [${bondLines.join("\n").replace(/^ {4}/, "")}]})`);
  out.push("");
}

out.push(`;; Every residue, keyed by its three-letter code, in teaching order.`);
out.push(`(def all`);
out.push(`  {${RESIDUES.map((c) => `:${c} ${c}`).join("\n   ")}})`);
out.push("");

process.stdout.write(out.join("\n"));
process.stderr.write(
  `generated ${RESIDUES.length} residues; formal charges: ` +
    charges.filter(([, q]) => q).map(([c, q]) => `${c}${q > 0 ? "+" : ""}${q}`).join(" ") + "\n",
);
