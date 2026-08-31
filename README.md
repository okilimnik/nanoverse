# nanoverse

Standalone browser visualizations for learning molecular biology — built to be
*looked at* and *poked at*, not just rendered. Written in ClojureScript
(compiled with [Squint](https://github.com/squint-cljs/squint)) and rendered
with [Babylon.js](https://www.babylonjs.com/).

Currently one visualization: the **Hydration Console** — one ethanol molecule,
six waters, and a live hydrogen-bond test you can drive with a slider.

---

## Quick start

Build output is **not** committed, so build once, then open the page:

```sh
npm install
npm run build
open web/hydration-console.html      # macOS
xdg-open web/hydration-console.html  # Linux
```

Requires Node (tested on v24). No JVM needed — Squint runs on plain Node.

Once built, it's a plain static HTML file with plain `<script src>` tags (no ES
modules, no import maps), so `file://` works — no dev server required.

If the canvas is blank, the likely cause is a missing build: the page loads
`web/js/hydration-console.js`, which only exists after `npm run build`.

**You do need network access on first load.** Babylon.js is pulled from a CDN
(cdnjs) rather than vendored. Offline, the page loads but the canvas stays
empty.

If you'd rather serve it over HTTP:

```sh
npm run serve
# then visit http://localhost:8000/hydration-console.html
```

One page, one renderer:

| File | Renderer | Source |
|---|---|---|
| `web/hydration-console.html` | Babylon.js (real WebGL 3D) | `src/nanoverse/hydration_console/babylon_core.cljs` |

---

## Driving the simulation

- **Drag the viewport** to orbit; scroll to zoom.
- **Brownian amplitude** (0 – 0.6 Å, default 0.28) — how far the waters wander.
  This is the main knob. At `0` the waters freeze and the four resting bonds
  stay lit; turn it up and bonds flicker on and off as hydrogens drift in and
  out of range.
- **Speed** (0 – 3×) — time scaling for the wobble.
- **Pause / Play**.
- The readout in the corner shows **H-bonds active** and the **nearest
  donor···acceptor distance** in Å, updated per frame.

If your OS has *reduce motion* enabled, the scene starts paused — press Play.

### What you're looking for

Grey = carbon, red = oxygen, white = hydrogen, grey sticks = covalent bonds.

A **cyan tube** appears between a hydrogen and an oxygen only when *both*
conditions hold:

- the donor–H···acceptor distance is under **2.5 Å**, and
- the D–H···A angle exceeds **120°** (i.e. roughly linear)

Concretely, with the default seed: at amplitude `0` exactly **4** bonds are lit
and stay lit. At the default `0.28` the count sits at 4 most of the time and
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

## Building from source

All commands run from the repo root.

| Script | What it does |
|---|---|
| `npm run build` | `compile` then `bundle` — the one you need after a `.cljs` edit |
| `npm run compile` | Squint only: `.cljs` → `out/*.mjs` |
| `npm run bundle` | esbuild only: `out/*.mjs` → `web/js/*.js` |
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
src/nanoverse/hydration_console/
  geometry.cljs                    chemistry + math — zero rendering dependency
  babylon_core.cljs                Babylon scene, meshes, render loop
web/
  hydration-console.html           page shell: CSS, DOM, controls, CDN <script> tags
  js/                              esbuild bundles  — generated, gitignored
out/                               squint output    — generated, gitignored
structures/
  ethanol.pdb                      reference structure (RCSB CCD, ligand EOH)
```

Namespaces are `nanoverse.<topic>.*`. Squint follows the ClojureScript
convention of mapping `-` in a namespace segment to `_` in the path, so
`nanoverse.hydration-console.geometry` lives at
`src/nanoverse/hydration_console/geometry.cljs`.

The `geometry.cljs` / `babylon_core.cljs` split is deliberate: the chemistry is
meant to be readable on its own, so the same verified math can be re-pointed at
a different renderer without touching it. An earlier Zdog renderer did exactly
that off the identical `geometry.cljs` before being removed — if you add
another, it goes beside `babylon_core.cljs` and `geometry.cljs` stays untouched.

To add a visualization: a new `src/nanoverse/<topic>/geometry.cljs` +
`babylon_core.cljs`, a `web/<topic>.html` shell (copy this one's CSS/DOM,
repoint the script tags), and one more `esbuild` line in `package.json`'s
`bundle` script.

Coordinates are **inlined** in `geometry.cljs` (ethanol from the RCSB Chemical
Component Dictionary ideal coordinates for EOH; water at standard gas-phase
geometry, O–H 0.9572 Å, H–O–H 104.5°). Nothing is fetched at runtime;
`structures/ethanol.pdb` is checked in as reference, not loaded by the page.

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
  "file://$PWD/web/hydration-console.html"
```

Two things that will waste your time otherwise:

- **Don't combine `--headless=new` with `--disable-gpu`** — that pairing leaves
  WebGL non-functional and the canvas comes out blank.
- **Always look at the actual pixels**, not just the console log. A script
  exception thrown before the first render produces *zero* console errors and a
  fully blank canvas. Without `--virtual-time-budget` the screenshot can also
  fire before the first frame is drawn — same blank canvas, nothing wrong.
