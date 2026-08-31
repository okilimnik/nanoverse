# nanoverse

Standalone browser visualizations for learning molecular biology — built to be
*looked at* and *poked at*, not just rendered. Written in ClojureScript
(compiled with [Squint](https://github.com/squint-cljs/squint)) and rendered
with [Babylon.js](https://www.babylonjs.com/).

One page, `web/index.html`, holding a **deck of slides** you move through with
the on-screen arrows, the dots, or the ← → keys. Two slides so far, both about
the same question — *what does water do around a small polar molecule?* —
approached from opposite sides:

- **Hydroxyl in Water** — ethanol, an alcohol. Its `–OH` both donates and
  accepts hydrogen bonds, so bonds run in both directions at once.
- **Aldehyde in Water** — acetaldehyde, what that ethanol becomes. Its `–CHO`
  is a carbonyl with an H on the same carbon, and neither half can donate —
  so it can only *accept*.

---

## Quick start

Build output is **not** committed, so build once, then open the page:

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

| # | Slide | Molecule | The point |
|---|---|---|---|
| 1 | Hydroxyl in Water | Ethanol | An `–OH` both gives and takes hydrogen bonds |
| 2 | Aldehyde in Water | Acetaldehyde | A `–CHO` accepts but never donates |

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
rest, ~1.65 at the default 50°, and ~0.9 at maximum tumble with full drift.

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
src/nanoverse/
  main.cljs                        entry point: the slide list
  deck.cljs                        slide navigation + shared Babylon helpers
  vec3.cljs                        shared vector + quaternion math
  hydroxyl/
    geometry.cljs                  chemistry + math — zero rendering dependency
    babylon_core.cljs              Babylon scene, meshes, render loop
  aldehyde/
    geometry.cljs
    babylon_core.cljs
web/
  index.html                       the deck: CSS, both slides' DOM, CDN <script> tags
  js/deck.js                       esbuild bundle   — generated, gitignored
out/                               squint output    — generated, gitignored
structures/
  ethanol.pdb                      RCSB CCD ideal coords, ligand EOH
  acetaldehyde.sdf                 PubChem CID 177 conformer (with bond orders)
```

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

Coordinates are **inlined** in each `geometry.cljs`, taken from the RCSB
Chemical Component Dictionary ideal structure for ethanol (`EOH`) and a
PubChem conformer for acetaldehyde, with water at standard gas-phase geometry (O–H
0.9572 Å, H–O–H 104.5°). Nothing is fetched at runtime — the files under
`structures/` are checked in for provenance, not loaded by any page.

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
  correct-looking screenshot; the bug only appeared on the first click. Serve
  the page (`npm run serve`) and drive it — `file://` is blocked by most
  automation tooling.
