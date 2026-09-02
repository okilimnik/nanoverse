# nanoverse

Standalone browser visualizations for learning molecular biology — built to be
*looked at* and *poked at*, not just rendered. Written in ClojureScript
(compiled with [Squint](https://github.com/squint-cljs/squint)) and rendered
with [Babylon.js](https://www.babylonjs.com/).

**▶ [Open the deck](https://okilimnik.github.io/nanoverse/)** — the live build,
nothing to install. Everything below is for running or changing it locally.

One page, `web/index.html`, holding a **deck of slides** you move through with
the on-screen arrows, the dots, or the ← → keys. Eight slides, one per
functional group, all asking the same question — *what does water do around
this group?* — and each built to be read against the one before it:

- **1–3 what the group can do with water.** An `–OH` gives *and* takes; swap it
  for a `–CHO` and it can only take; move that same `C=O` into the middle of
  the chain and you get a ketone.
- **4–6 what pH does to the group.** A `–COOH` loses a proton and goes
  negative, an `–NH2` gains one and goes positive, and a phosphate does it
  three times over.
- **7–8 what the element itself decides.** Swap oxygen for the sulfur below it
  in the periodic table, and then look at a group that does nothing at all.

---

## Quick start

To run it yourself — which you need for editing, but not for looking — build
once, then open the page. Build output is **not** committed:

```sh
npm install
npm run build
open web/index.html   # macOS ("open" -> "xdg-open" on Linux)
```

Requires Node (tested on v24). No JVM needed — Squint runs on plain Node.

Once built, it's a plain static HTML file with plain `<script src>` tags (no ES
modules, no import maps), so `file://` works — no dev server required.

If the canvas is blank, the likely cause is a missing build: the page loads
`web/js/deck.js`, which only exists after `npm run build`.

**You do need network access on first load.** Babylon.js is pulled from a CDN
(cdnjs) rather than vendored. Offline, the page loads but the canvas stays
empty.

If you'd rather serve it over HTTP:

```sh
npm run serve
# then visit http://localhost:8000/
```

| # | Slide | Molecule | Knob | The point |
|---|---|---|---|---|
| 1 | Hydroxyl in Water | Ethanol | amplitude | An `–OH` both gives and takes hydrogen bonds |
| 2 | Aldehyde in Water | Acetaldehyde | tumble | A `–CHO` accepts but never donates |
| 3 | Keto in Water | Acetone | shade the methyls | Same `C=O` mid-chain — twice the greasy surface |
| 4 | Carboxyl in Water | Acetic acid ⇌ acetate | **pH** | An acid: loses a proton, goes negative |
| 5 | Amino in Water | Methylamine | **pH** | A base: gains one, and spends its lone pair |
| 6 | Phosphate in Water | 3-phosphoglycerate | **pH** | Three pKas — charge as a design tool |
| 7 | Sulfhydryl in Water | Mercaptoethanol ⇌ disulfide | bond strength, oxidise | Sulfur is not oxygen |
| 8 | Methyl in Water | Alanine | **pH**, shade the side chain | A group that does nothing, and why that matters |

Only the slide you're looking at runs a render loop — the others are paused,
and a slide you haven't opened yet hasn't been built at all.

---

## Slide 1 — Hydroxyl in Water (ethanol)

- **Drag the viewport** to orbit; scroll to zoom.
- **Brownian amplitude** (0 – 0.6 Å, default 0.28) — how far the waters wander.
  This is the main knob. At `0` the waters freeze and the four resting bonds
  stay lit; turn it up and bonds flicker on and off as hydrogens drift in and
  out of range.
- **Speed** (0 – 3×) — time scaling for the wobble.
- **Pause / Play**.
- Readouts: **H-bonds active**, **–OH gives** (ethanol's hydroxyl hydrogen
  donating outward), **–OH takes** (its oxygen accepting a water's hydrogen),
  and the **nearest donor···acceptor distance** in Å.

If your OS has *reduce motion* enabled, the scene starts paused — press Play.

### What you're looking for

Grey = carbon, red = oxygen, white = hydrogen, grey sticks = covalent bonds.

A **cyan tube** appears between a hydrogen and an oxygen only when *both*
conditions hold:

- the donor–H···acceptor distance is under **2.5 Å**, and
- the D–H···A angle exceeds **120°** (i.e. roughly linear)

Concretely, with the default seed: at amplitude `0` exactly **4** bonds are lit
and stay lit — **1** running outward from ethanol's own hydroxyl hydrogen and
**3** running inward to its oxygen. That split is the hydroxyl group's whole
character, and it is what the next slide's `C=O` cannot do. At the default `0.28` the count sits at 4 most of the time and
wanders between **2 and 5**. At the maximum `0.6` it ranges **0 – 5** and
occasionally drops to zero — the ethanol briefly loses its whole hydration
shell. Watching that number breathe as you push the slider is the thing to
watch: hydration is a shell that's constantly being made and unmade, not a
fixed set of attachments.

> **Be careful what you conclude from this scene.** The two-part test
> (distance *and* angle) is what a real hydrogen-bond criterion looks like, and
> the code implements both halves — but **in this scene the angle half never
> actually rejects anything**. Each water is constructed with one O–H aimed
> straight at ethanol's oxygen, and the wobble only *translates* the waters, it
> never rotates them. So every pair that comes within range is linear to within
> about 0.3°. The count you're watching is driven purely by distance. Seeing
> the angle criterion do real work would need rotational motion, which this
> scene doesn't have.

### Limits of the model

- The wobble is a **hand-tuned sum of sine waves**, seeded per atom. It looks
  like thermal jitter but it is **not** a physics simulation. Nothing repels or
  collides; molecules can drift through each other.
- The bond test is the standard **geometric proxy** (distance + angle), not an
  electron-density calculation.
- Waters **translate but never rotate**, so the angle half of the test is inert
  here (see the note above) — what you see is a distance cutoff.
- Water's lone pairs aren't modeled as directional, so only the donor side of
  each candidate pair is checked.
- Resting O···O separations were **hand-tuned** so the default amplitude puts
  bonds right at the edge of the cutoff. The flickering is calibrated to be
  visible, not measured from anything.

The seeded PRNG means the motion is identical across reloads — same wobble
every time, so a thing you noticed is still there when you look again.

---

## Slide 2 — Aldehyde in Water (acetaldehyde)

An aldehyde instead of an alcohol, and the slide that actually makes the
*angle* half of the hydrogen-bond test do visible work.

- **Brownian amplitude** (0 – 0.6 Å, default 0.30) — how far the waters drift.
  Secondary here; tumble is the knob that matters.
- **Tumble** (0 – 70°, default 50) — how far each water *rotates* about its own
  axis. This is the knob slide 1 doesn't have, and the reason the angle test
  does visible work on this slide.
- **Speed** (0 – 3×), **Pause / Play**.
- **Show contacts rejected for angle** — draws the near-misses as dull red
  sticks.

Readouts: **H-bonds active**, **–CHO gives**, **–CHO takes**, and **close but
too bent** — the same gives/takes pair as slide 1, so the two are directly
comparable.

### What you're looking for

Cyan = a hydrogen bond at the carbonyl oxygen. Amber = one at an O–H. Dull red
= close enough to touch but pointing the wrong way, so not a bond at all.

**Watch GIVES.** It is pinned at 0 and stays there across the entire control
range — not hardcoded, but because acetaldehyde has no polar hydrogen to offer,
so no candidate pair can put it on the donating side. Slide 1's ethanol, the
same size, sat at 1. That digit is the whole alcohol/aldehyde difference.

**Set Tumble to 0.** Both bonds lock on and stay on. Now raise it: bonds start
dying *without the molecules moving apart* — the hydrogen swings off-axis and
the geometry fails on angle alone. With the default seed the count runs 2.0 at
rest, ~1.57 at the default 50°, and ~0.85 at maximum tumble with full drift.

**Look at the methyl end.** Two waters sit there and never bond — verified
across the whole slider range, they form exactly zero bonds and never get
closer than 3.5 Å to the carbonyl oxygen. A `–CH3` has no lone pair to accept
with and no polar hydrogen to give.

The carbonyl oxygen accepts at most **two** bonds, one per sp2 lone pair,
against ethanol's four. The aldehyde's own C–H never bonds either — carbon
isn't electronegative enough to be a donor, so it is drawn but never tested.

### Limits of the model

- Drift and tumble are both **hand-tuned sums of sine waves**, seeded per
  water. Not a physics simulation: nothing repels, collides, or exchanges, and
  the molecule itself is held rigid. Real hydration-shell waters swap out on a
  picosecond timescale; these six never leave.
- Lone-pair directions are used to *place* the two carbonyl waters (sp2, 120°
  from the O→C direction, computed from the real geometry), but the bond test
  itself doesn't check the angle at the acceptor — so a water can wander
  somewhere a real lone pair wouldn't accept it.
- Nothing repels, so pushing amplitude to its maximum drives a water's oxygen
  to ~2.1 Å from the carbonyl oxygen — closer than two oxygens ever really
  get. The slider is capped at 0.6 Å to keep that from getting absurd, but the
  overlap is visible at the top of the range.
- **Provenance is weaker here than on slide 1.** Acetaldehyde is too small and
  volatile to exist as a PDB ligand, so these are **computed** coordinates
  (PubChem CID 177), not experimental ones. They reproduce the published
  microwave structure closely — C=O 1.226 vs 1.216 Å, C–C 1.498 vs 1.501 Å,
  C–C=O 123.4° vs 124.1° — and the CHO group is planar to 0.0007 Å, which is
  what the lone-pair construction depends on. Kept in
  `structures/acetaldehyde.sdf`; the `C=O` is drawn as a double line because
  the SDF says order 2, not because a list names that bond.

---

## Slide 3 — Keto in Water (acetone)

The same carbonyl as slide 2, moved. An aldehyde puts its `C=O` at the *end* of
a chain with a hydrogen on the carbonyl carbon; a ketone puts it in the
*middle*, with a carbon on each side.

- **Brownian amplitude**, **Tumble**, **Speed**, **Pause / Play**, **Show
  contacts rejected for angle** — as slide 2.
- **Shade the two methyl groups** — recolours the greasy half of the molecule
  so you can see how much of acetone it is.
- Readouts: **H-bonds active**, **C=O gives** (0), **C=O takes**, **–CH₃
  bonds** (0), **close but too bent**, plus the measured `C=O` length and the
  number of waters parked against the methyls.

### What you're looking for

**GIVES is zero again, for a new reason.** The aldehyde had a hydrogen on its
carbonyl carbon and still couldn't donate, because a C–H isn't polar enough.
Acetone doesn't even have that — all six of its hydrogens sit on methyl
carbons. Two bonds in, on the oxygen's two sp2 lone pairs, nothing out.

**Then count the grease.** Four waters sit against the two methyls and never
bond at any amplitude. The aldehyde had one methyl and a bare hydrogen; the
ketone has two methyls. Moving the `C=O` inward didn't change what the oxygen
does — it doubled the oily surface around it.

**The difference you can't see here** is the one an exam asks about: an
aldehyde's carbonyl carbon still carries a hydrogen, so it can be oxidised one
more step into slide 4's carboxyl group. A ketone's carbonyl carbon is bonded
to two carbons and has nothing left to give up — it's a dead end.

### Limits of the model

Same hand-tuned sine-wave motion and same geometric bond test as slides 1–2.
Coordinates are the **ideal (computed)** conformer of PDB component `ACN` —
acetone *is* a deposited component, unlike acetaldehyde, but "ideal" still
means computed rather than measured. The angle at the *acceptor* isn't checked.

---

## Slide 4 — Carboxyl in Water (acetic acid ⇌ acetate)

A carbonyl and a hydroxyl on the same carbon. Put those two together and the
O–H becomes acidic enough to leave. **The first slide with a pH knob.**

- **pH** (0 – 14, default 7) — the main knob. Crossing `pKa 4.76` swaps one
  deposited structure for another.
- Amplitude, tumble, speed, play, show-bent as before.
- Readouts: bonds active / **gives** / **takes** / bent, plus **species**,
  **% ionised**, **net charge**, and the two **C–O bond lengths**.

### What you're looking for

**Drag through 4.76.** The hydroxyl hydrogen disappears, a red charge cloud
spreads over both oxygens, and **GIVES drops 1 → 0**. An acid that has already
given its proton away has nothing left to give. **TAKES goes up** at the same
time — a negative oxygen is a better acceptor than a neutral one.

**Watch the C–O lengths.** Below the pKa: `1.208 / 1.342 Å`, one double bond
and one single. Above it: `1.220 / 1.220 Å`, *identical*. That is not a redraw
— it is two independently deposited structures (`ACY` and `ACT`) reporting the
same thing, read live off their coordinates. Losing the proton shares the
charge across both oxygens and the two bonds become one kind of bond, which is
why the anion is drawn with **one-and-a-half** bonds even though the CIF's
bond-order column still says "double" and "single". A CIF has to pick a
Kekulé structure; the coordinates don't.

**Set pH 7.** Over 99% ionised. When a textbook says "carboxyl group", the
thing in the cell is the carboxyl*ate* — which is why aspartate and glutamate
carry a minus sign.

### Limits of the model

- **One molecule can't show a mixture.** The drawing steps at the pKa; a real
  solution at pH 4.76 is a 50/50 blend with every molecule swapping constantly.
  The **% ionised** readout (Henderson–Hasselbalch) is the honest, continuous
  version of what the picture simplifies.
- `pKa 4.76` is dilute water at 25 °C. The same group buried in a protein can
  shift several units.
- **The proton doesn't vanish** — it makes H₃O⁺, which isn't drawn. Neither is
  any counter-ion, so charge isn't conserved on screen.
- Charge clouds are **formal-charge bookkeeping** (half a minus per oxygen,
  the way resonance is taught), not computed electron density.

---

## Slide 5 — Amino in Water (methylamine)

The same slider as slide 4, run in the opposite direction. A carboxyl is an
acid that loses a proton and goes negative; an amine is a base that takes one
and goes positive — and pays for it.

- **pH** (0 – 14, default 7), crossing `pKa 10.66`. Other controls as before.
- Readouts: bonds / gives / takes / bent, plus **species**, **% protonated**,
  **charge**, and **N lone pairs**.

### What you're looking for

**Watch TAKES, not GIVES.** Below the pKa the amine picks up a third hydrogen:
GIVES climbs 2 → 3 and **TAKES falls to zero**. The nitrogen's lone pair was
what did the accepting, and a proton is now bonded to it. An amine trades away
its ability to accept for one more hydrogen to give, and cannot do both.

That isn't special-cased. The nitrogen goes through the same
`solvent/lone-pair-dirs` routine as every acceptor in the deck, and a nitrogen
with four neighbours simply has no free pair to find. **N LONE PAIRS** is that
routine's answer, not a label.

**Set pH 7.4** — blood. About 99.9% protonated. Every lysine side chain and
every protein N-terminus is a cation for this reason. Read it against slide 4
at the same pH: at cell pH acids are negative and bases are positive
*simultaneously*, which is what slide 8 puts on one molecule.

### Limits of the model

- **The third N–H is constructed, not measured.** `NME` is the neutral amine;
  the ammonium form places a hydrogen at the one tetrahedral vertex the other
  three bonds leave open, at a standard `1.010 Å`, with the rest of the
  molecule *not* re-relaxed. Slide 4, where both forms are separately
  deposited, needed no such construction.
- Same majority-species step, same missing counter-ion (here an OH⁻), same
  geometric bond test and sine-wave motion as slide 4.

---

## Slide 6 — Phosphate in Water (3-phosphoglycerate)

A real metabolite — the product of glycolysis' first ATP-making step — carrying
**three** ionisable protons plus one hydroxyl that never ionises.

- **pH** (0 – 14, default 7.2), crossing `pKa ≈ 1.42, 3.42, 6.21`.
- Readouts: bonds active and how they split between **phosphate / carboxyl /
  –OH**, plus **species**, **net charge**, **protons released**, **majority
  species %**, and whether this is the cell's form.

### What you're looking for

**Walk the slider and watch NET CHARGE** step `0 → −1 → −2 → −3`. By cell pH
this small molecule carries three negative charges. That is what a phosphate
group is *for*: charge is a tag, and something this charged can't drift out
through the greasy middle of a membrane. Phosphorylate a molecule and you've
locked it inside the cell — and handed every enzyme a big distinctive handle.

**The hydration shell grows with the charge.** That's the visible half of why
phosphorylation changes a protein so completely: not one added atom, but a
charged, heavily hydrated lump where a plain hydroxyl used to be.

**GIVES never reaches zero.** Even at pH 14 one hydrogen remains — the hydroxyl
on the middle carbon, a plain secondary alcohol with a pKa around 15, off the
top of the slider. Three of this molecule's four O–H groups look identical in
the structure file and one behaves completely differently. Where a hydrogen
sits doesn't tell you whether it will leave; what it's attached to does.

**Park on pH 6.2** and MAJORITY SPECIES reads ~50%. The picture is showing one
molecule and having to pick.

### Limits of the model

- **The pKa values are approximate** literature ballparks for a phosphate
  monoester next to an α-hydroxy acid. Real values shift a few tenths with
  ionic strength, temperature and counter-ion, and sources disagree at that
  level. The *shape* of the ladder is the point.
- **The coordinates are the fully protonated acid's throughout.** Removing a
  proton removes an atom and nothing else — no bond length is re-relaxed. The
  P=O stays `1.480 Å` and the two P–OH stay `1.609 Å` even at −3, where all
  three should have equalised near 1.5. The carboxylate has the same problem:
  its bond *order* is redrawn one-and-a-half, its *lengths* are still the
  acid's. Slide 4, which has both forms deposited, shows what that relaxation
  really looks like.
- **No counter-ions.** In a cell this ion is almost never alone — Mg²⁺ sits on
  phosphates constantly. Treat the water shell as an upper bound.

---

## Slide 7 — Sulfhydryl in Water (mercaptoethanol ⇌ disulfide)

2-mercaptoethanol carries an `–OH` on one carbon and an `–SH` on the next: the
same group with oxygen swapped for the element directly below it. One molecule
and its own control.

- **Minimum bond strength** (0 – 22 kJ/mol) — filters the drawn bonds by a
  *tabulated* typical enthalpy for their donor/acceptor element pair.
- **Oxidise — form the S–S bridge** — swaps the two thiols for the real
  disulfide.
- Amplitude, tumble, speed, play, show-bent as before.
- Readouts: bonds at the **–OH** vs at the **sulfur**, a tabulated total, plus
  the measured `C–O`, `C–S` and `S–S` lengths.

### What you're looking for

**Compare the bond lengths.** `C–O 1.430 Å` against `C–S 1.813 Å` — the same
kind of single bond, from the same deposited structure, nearly four tenths of
an ångström longer. Sulfur is a much bigger atom, which is why it's drawn
bigger and most of why a thiol isn't an alcohol.

**Both ends light up — and that is this deck's bond test failing.** The
geometric proxy asks only about distance and angle, and by those measures the
S–H contact is a perfectly good hydrogen bond. It isn't: sulfur's
electronegativity is 2.58 against oxygen's 3.44, so an S–H bond is barely
polarised. **Raise the minimum-strength slider** and the sulfur bonds die first
while the `–OH` ones stay lit. That slider is both the lesson and the
confession — the strengths behind it are looked up in a table precisely
*because* the geometry on screen can't tell the two apart.

**Tick oxidise.** Two thiols become one molecule joined by a real `S–S` bridge
of `2.050 Å`, and the S–H hydrogens are simply gone. This is the bond that
staples an antibody together and holds keratin rigid, and it's reversible under
ordinary cellular conditions, which no C–O or C–C bond is. Oxygen has no
equivalent. Meanwhile the `–OH` ends don't change at all: half the molecule is
having a redox reaction and the other half doesn't notice.

### Limits of the model

- **The reduced pair's pose is constructed.** Each half is a real, whole `BME`
  (with its own real S–H) and the disulfide is a real `HED` — but the two free
  thiols are drawn in the pose their disulfide would have, pulled apart along
  the S–S axis, so you can see which bond formed. In solution two free thiols
  have no fixed relationship at all.
- **The toggle is not a reaction.** It swaps structures. A real oxidation needs
  an oxidant, releases 2 H⁺ and 2 e⁻, and goes through a mixed intermediate.
  Nor is the reverse modelled: mercaptoethanol breaks a protein's disulfide by
  *exchanging* with it in two steps.
- **The strength numbers are tabulated ballparks** for neutral donor/acceptor
  element pairs (~21 kJ/mol for O–H···O, ~7 for S–H···O). They don't vary with
  distance or angle and charged partners would be much stronger. The total is a
  ranking device, not an energy.
- Thiols also **ionise** — a cysteine thiol's pKa is around 8.3, low enough to
  matter at cell pH, and the thiolate is the reactive species in most enzymes
  that use one. Not on this slide; the pH slider from slides 4–6 would be the
  way to show it.

---

## Slide 8 — Methyl in Water (alanine)

The last slide, and the only one whose group does nothing: no lone pair, no
polar hydrogen, no pH at which it ionises, no charge ever.

- **pH** (0 – 14, default 7.4), crossing `pKa 2.34` (carboxyl) and `9.69`
  (amino).
- **Shade the methyl side chain**, plus the usual amplitude / tumble / speed /
  play / show-bent.
- Readouts: bonds at the **amino end**, the **carboxyl end**, and the
  **–CH₃** — plus species, net charge, and what each end is carrying.

### What you're looking for

**The readout that never moves.** Bonds at the `–CH₃` stay at **zero** at every
pH and every amplitude, with four waters parked against the side chain
permanently. Every other slide has a number that responds to the knobs; this
one is here because it doesn't.

**Move the pH slider and watch the rest.** The carboxyl end deprotonates at
2.34 and the amino end at 9.69 — slides 4 and 5 replayed on one molecule, in
order. The methyl between them notices neither.

**Stop at pH 7.4 and read ENDS.** Net charge is zero but the molecule is `+1`
at one end and `−1` at the other: a **zwitterion**. That's what every free
amino acid is in water, and why they're high-melting solids rather than oily
liquids. Net-zero is not the same as uncharged.

**Why the nothing matters.** Water can't hydrogen bond to a methyl, so the
waters beside one have to satisfy their bonds among *themselves*, in a more
ordered arrangement than bulk water. Order costs entropy, and the system pays
less of it by pushing the greasy surfaces together, out of the water. That
shove is the **hydrophobic effect** — it folds protein cores, holds every
membrane together, and keeps oil out of vinegar.

### Limits of the model

- **The most important limit in the deck is on this slide.** That ordering is
  *not modelled*. The methyl's waters are placed at van der Waals contact and
  jiggled like every other water; real hydration-shell waters against a
  hydrophobic surface adopt tangential orientations so they can keep bonding to
  each other. The entropy cost that *is* the hydrophobic effect doesn't appear
  here at all. What you can see is only the **precondition** — the absence of a
  bond. The force itself is statistical and entropic, and no single-molecule
  snapshot can show it.
- **The third N–H is constructed**, as on slide 5: `ALA` is the neutral form.
  The carboxylate's bond *order* is redrawn one-and-a-half, its *lengths* are
  still the acid's.
- pKa 2.34 and 9.69 are free alanine in dilute water; both shift once the
  residue is in a peptide. Same majority-species step, geometric bond test and
  sine-wave motion as the rest of the deck.

---

## Building from source

All commands run from the repo root.

| Script | What it does |
|---|---|
| `npm run build` | `compile` then `bundle` — the one you need after a `.cljs` edit |
| `npm run compile` | Squint only: `.cljs` → `out/*.mjs` |
| `npm run bundle` | esbuild only: `out/nanoverse/main.mjs` → `web/js/deck.js` |
| `npm run watch` | both, re-running on every save |
| `npm run serve` | static server on `web/` at :8000 |
| `npm run clean` | delete `out/` and `web/js/` |

The bundle step uses `--format=iife`, which is what keeps the output loadable
from `file://` — an ES-module build would not be.

`npm run watch` runs `squint watch` and `esbuild --watch=forever` side by side,
so `web/js/` stays current; just reload the page. (The `=forever` matters —
plain `--watch` quits the moment stdin closes, which silently leaves you
reloading a stale bundle.)

### Layout

```
package.json                       scripts: build / compile / bundle / watch / serve / clean
squint.edn                         compiles everything under src/ in one pass
.github/workflows/pages.yml        builds + publishes web/ to GitHub Pages
src/nanoverse/
  main.cljs                        entry point: the slide list
  deck.cljs                        slide navigation + shared Babylon helpers
  vec3.cljs                        shared vector + quaternion math
  solvent.cljs                     the water model — zero rendering dependency
  scene.cljs                       shared Babylon scene builder for the group slides
  hydroxyl/
    geometry.cljs                  chemistry + math — zero rendering dependency
    babylon_core.cljs              Babylon scene wiring + this slide's readouts
  aldehyde/    …  ketone/     …  carboxyl/   …    (the same two files for each
  amino/       …  phosphate/  …  sulfhydryl/ …     of the eight topics)
  methyl/      …
web/
  index.html                       the deck: CSS, all eight slides' DOM, CDN <script>
  js/deck.js                       esbuild bundle   — generated, gitignored
out/                               squint output    — generated, gitignored
structures/                        provenance only — never loaded at runtime
  ethanol.pdb                      RCSB CCD ideal coords, ligand EOH
  acetaldehyde.sdf                 PubChem CID 177 conformer (with bond orders)
  ACN.cif  ACY.cif  ACT.cif        acetone; acetic acid and acetate
  NME.cif  3PG.cif                 methylamine; 3-phosphoglyceric acid
  BME.cif  HED.cif  ALA.cif        mercaptoethanol, its disulfide; alanine
```

All eight slides share two namespaces beyond `vec3` and `deck`. **`solvent.cljs`** is
the water model — lone-pair directions computed from a molecule's own
coordinates, automatic hydration-shell placement, the geometric H-bond test,
`protonate` / `deprotonate`, a rigid three-point `align` for states that come
from two different CCD entries, and Henderson–Hasselbalch. It has no rendering
dependency, exactly like `vec3.cljs`. **`scene.cljs`** is its Babylon half:
engine and materials, drawing a molecule from `{:atoms :bonds :charges}`,
building one *state group* per protonation or redox state, the per-frame
update, and the standard control wiring. Every state is built once up front and
switching is a visibility flip, so dragging a pH slider never rebuilds a mesh.

The effect is that a slide's own two files stay small and stay about chemistry:
`geometry.cljs` declares real coordinates plus what the molecule can do
(`:acceptors`, `:donors`, `:hydrophobic`) and hands them to `solvent/hydrate`;
`babylon_core.cljs` wires that slide's particular knob and reports the counts
that slide cares about. Both `babylon_core.cljs` files for slides 1–2 went from
~200 lines to ~35 when they were re-pointed at this, and the bundle lost 24 kB.

Slides 1–2 use `solvent/place` rather than `solvent/hydrate`: they keep their
hand-placed waters, because slide 1's resting distances are *calibrated* to sit
just inside the bond cutoff so the count visibly breathes, and that flicker is
the lesson. It's the documented exception, not the pattern — everything else
lets the lone-pair geometry decide.

Namespaces are `nanoverse.<topic>.*`. Squint follows the ClojureScript
convention of mapping `-` in a namespace segment to `_` in the path, so
`nanoverse.hydroxyl.geometry` lives at
`src/nanoverse/hydroxyl/geometry.cljs`.

The `geometry.cljs` / `babylon_core.cljs` split is deliberate: the chemistry is
meant to be readable on its own, so the same verified math can be re-pointed at
a different renderer without touching it. An earlier Zdog renderer did exactly
that off the identical `geometry.cljs` before being removed — if you add
another, it goes beside `babylon_core.cljs` and `geometry.cljs` stays untouched.

To add a slide: a new `src/nanoverse/<topic>/geometry.cljs` +
`babylon_core.cljs` exposing `(build prefix)`, a
`<section class="slide" id="slide-<topic>">` in `web/index.html` with its ids
prefixed, and one more entry in `nanoverse.main`'s slide vector. The build
scripts don't change — there is only one bundle.

Coordinates are **inlined** in each `geometry.cljs`. All but one come from the
RCSB Chemical Component Dictionary — `https://files.rcsb.org/ligands/download/<ID>.cif`
gives ideal coordinates, elements *and* bond orders in a single file, which the
`_ideal.sdf` doesn't and an `_ideal.pdb` doesn't exist for. The exception is
acetaldehyde, which is too small and volatile to be a deposited component and
comes from a PubChem conformer instead. Water is standard gas-phase geometry
(O–H 0.9572 Å, H–O–H 104.5°). Nothing is fetched at runtime — the files under
`structures/` are checked in for provenance, not loaded by any page.

Where a slide needs two states, it prefers **two deposited components** over
deriving the second by hand: acetic acid / acetate are `ACY` / `ACT`, and
mercaptoethanol / its disulfide are `BME` / `HED`. Two CCD entries arrive in
unrelated coordinate frames, so `solvent/align` puts one on the other before
anything else — otherwise crossing a pKa would spin the molecule, which reads
as chemistry when it's only bookkeeping. Where no second component exists,
removing an atom (exact) is preferred to adding one (constructed), and every
slide says on its face which it did.

### Deploying

Pushing to `master` publishes the deck to
[GitHub Pages](https://okilimnik.github.io/nanoverse/) via
`.github/workflows/pages.yml`. Since `web/js/` is gitignored, CI runs the same
`npm ci && npm run build` you would run locally and uploads `web/` as the Pages
artifact — the deployed site is always built from source, and no generated file
is ever committed.

The repo's Pages source has to be set to **GitHub Actions** (not "deploy from a
branch") or `configure-pages` fails with a 404.

---

## Verifying a change headlessly

Babylon needs a real WebGL context, so a screenshot check has to give it one.
This flag set is confirmed working on macOS:

```sh
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  --headless=new \
  --use-gl=angle --use-angle=swiftshader --enable-unsafe-swiftshader \
  --virtual-time-budget=4000 \
  --window-size=900,1000 \
  --screenshot=/tmp/shot.png \
  "file://$PWD/web/index.html"
```

Two things that will waste your time otherwise:

- **Don't combine `--headless=new` with `--disable-gpu`** — that pairing leaves
  WebGL non-functional and the canvas comes out blank.
- **Always look at the actual pixels**, not just the console log. A script
  exception thrown before the first render produces *zero* console errors and a
  fully blank canvas. Without `--virtual-time-budget` the screenshot can also
  fire before the first frame is drawn — same blank canvas, nothing wrong.
- **A screenshot only ever tests slide 1.** Navigation, and anything else that
  needs a click or a keypress, has to be driven in a real browser. The
  slide-dot handlers shipped broken past a clean build, a clean console, and a
  correct-looking screenshot; the bug only appeared on the first click.

### Driving the whole deck

For anything past slide 1, script a browser instead. Playwright works against
`file://` here and needs the same WebGL flags:

```js
const browser = await chromium.launch({
  args: ['--use-gl=swiftshader', '--enable-unsafe-swiftshader'],
});
```

Click `#deck-dots .dot` to reach slide *n*, then set a control and read the
DOM back — the readouts are the assertion surface, and they are cheaper and far
more specific than comparing pixels:

```js
const set = (id, v) => page.evaluate(([id, v]) => {
  const e = document.getElementById(id);
  e.value = v;                                     // .checked for a toggle
  e.dispatchEvent(new Event('input', { bubbles: true }));   // 'change' for a toggle
}, [id, v]);

await set('cbx-ph', 2);     // acetic acid:  gives 1, C–O 1.208 / 1.342 Å
await set('cbx-ph', 9);     // acetate:      gives 0, C–O 1.220 / 1.220 Å
```

One trap when you *do* want pixels: reading the canvas back with `drawImage`
into a 2D context returns **blank**, because Babylon doesn't set
`preserveDrawingBuffer` and the buffer is cleared after compositing. That looks
exactly like a dead scene and isn't one. Use Playwright's `.screenshot()`,
which goes through the compositor, and measure that instead.

Cheaper still: the chemistry has **no rendering dependency at all**, so a
`geometry.cljs` can be imported straight into Node from `out/` and checked
without a browser — resting bond counts, waters placed per site, charge per
protonation state. Most mistakes show up there first.

```sh
npm run build
node -e "import('./out/nanoverse/carboxyl/geometry.mjs').then(m =>
  console.log(m.states.map(s => [s.label, s.charge, s.waters.length])))"
```
