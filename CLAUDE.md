# nanoverse

A learning project. The user is studying **molecular biology** — chemical bonding,
molecular interactions, structure — and learns by *seeing* things, built as
standalone browser visualizations written in **ClojureScript** (compiled with
**Squint**) and rendered with **Babylon.js**.

## What "done" looks like here

The goal is understanding, not renders. A deliverable is good when it makes a
chemical idea visible and manipulable, and when the user can change one thing and
watch the consequence.

- Prefer **real structural data** (fetch from the PDB) over invented geometry.
  Chemistry that is made up teaches nothing. The PDB Chemical Component
  Dictionary is the workhorse: `https://files.rcsb.org/ligands/download/<ID>.cif`
  carries ideal coordinates, elements *and* bond orders in one file, which is
  more than the `_ideal.sdf` or the (nonexistent) `_ideal.pdb` give you. Where
  a slide needs two states, look for two components — acetic acid and acetate
  are `ACY`/`ACT`, mercaptoethanol and its disulfide are `BME`/`HED` — rather
  than deriving the second one by hand. When you do have to derive one, say so
  on the slide, and prefer removing an atom (exact) to adding one (constructed).
- Drive appearance from **per-atom attributes** so the picture is derived from the
  data, not hand-painted.
- Leave a **knob** the user can turn (a slider, a control) rather than a
  hard-coded look.
- **State the limits of the model.** A distance-only hydrogen-bond proxy is not a
  real electron-density calculation; a hand-tuned sine-wave wobble is not a
  physics simulation. Say so — a wrong mental model is worse than no picture.

## Web visualizations

Visualizations are slides in a single deck: **one** page, `web/index.html`,
with arrow / dot / keyboard navigation between slides. They are written in **ClojureScript**, compiled
with **Squint** (`squint-cljs` — a lightweight CLJS-to-JS compiler that runs on
plain Node, no JVM), and rendered with **Babylon.js** (loaded from cdnjs as a
classic global script exposing `window.BABYLON` — the plain-`<script src>`
build, not an ES module).

### Toolchain

The npm project is the repo root — `package.json`, `squint.edn`, and `src/` all
sit at the top level, so `npm run build` works from wherever you cloned to.

- `squint.edn` — compiles every `.cljs` under `src/` in one pass into `out/`.
- `package.json` — `npm run build` = `compile` (squint) then `bundle` (esbuild
  `--bundle --format=iife` over `nanoverse.main`, into `web/js/deck.js`). One
  bundle for the whole deck. The IIFE format matters: the page is a plain
  `<script src>` with no `<script type="module">` and no import maps, so it
  still opens directly via `file://` like any static HTML file.
- `npm run watch` runs squint and esbuild together, so `web/js/` stays fresh on
  every save. `npm run serve` serves `web/` over HTTP; `npm run clean` drops
  both generated dirs.
- Sources are namespaced under `nanoverse.<topic>.*`, so
  `src/nanoverse/<topic>/geometry.cljs` is `nanoverse.<topic>.geometry`
  (note Squint, like ClojureScript, maps the `-` in a namespace segment to `_`
  in the directory name).
- `<topic>/geometry.cljs` — pure chemistry/physics data and math for a given
  visualization, with **zero rendering-engine dependency**.
  `<topic>/babylon_core.cljs` `:require`s it and does only the Babylon-specific
  parts (scene/camera/mesh construction, the per-frame render loop). Keeping
  these separate means the same verified chemistry can be re-pointed at a
  different renderer later without touching the math.
- `nanoverse/vec3.cljs` — shared vector and quaternion helpers. Every
  `geometry.cljs` builds on it rather than re-deriving `add`/`sub`/`cross`;
  add new general-purpose math here, not in a topic namespace. Quaternions
  exist so a rotating body's geometry and its rendered transform are handed
  the *same four numbers* and cannot disagree about Euler-angle ordering.
- `nanoverse/solvent.cljs` — the water model every "functional group in water"
  slide shares, and still **zero rendering dependency**: lone-pair directions
  computed from a molecule's own coordinates, automatic hydration-shell
  placement, the geometric H-bond test, `protonate`/`deprotonate`, rigid
  three-point `align`, and Henderson–Hasselbalch. A slide should never
  hand-place a water — hand-placing means the picture shows what the author
  expected. `place` is the escape hatch for a slide that is *deliberately*
  calibrating its shell and says so on its face (only the hydroxyl slide, whose
  tuned resting distances make the count breathe); reach for `hydrate` first. Note the ordering inside `hydrate`: **donor waters are placed
  before lone-pair waters**, because a donor's water has exactly one possible
  position (fixed by a measured bond direction) while a lone pair has two or
  three alternatives. Reverse that and a molecule's single acidic proton
  silently loses its water to a lone pair that had somewhere else to go.
- `nanoverse/scene.cljs` — the Babylon half of the same pattern: engine,
  camera, materials, drawing a molecule from `{:atoms :bonds :charges}`,
  building a state group, the per-frame update, and the standard control
  wiring. A slide hands it one or more **states** (protonation, redox); every
  state is built once up front and switching is a visibility flip, so dragging
  a pH slider never rebuilds a mesh. `titration!` wires a pH slider to an
  ordered ladder of states.

- `nanoverse/deck.cljs` — shared Babylon helpers plus the slide navigation.
  Knows nothing about any specific slide, which is what keeps it free of a
  require cycle; `nanoverse/main.cljs` is the single entry point that pairs the
  deck with its slide list.
- A slide's `babylon_core.cljs` exposes **`(build prefix)`** and does nothing at
  module load. It creates its own engine on its own canvas and returns a handle
  via `deck/slide-handle`. Slides are built lazily on first visit, and only the
  visible one runs a render loop or holds the camera's pointer control —
  otherwise two cameras fight over the same drag.
- Every DOM id inside a slide is namespaced by that slide's prefix (`oh-amp`,
  `ald-amp`), since all slides share one document.

Generated output (`out/`, `web/js/`) is gitignored — a fresh clone must run
`npm install && npm run build` before any page will render.

To add a slide: new `src/nanoverse/<topic>/geometry.cljs` + `babylon_core.cljs`
exposing `build`, a `<section class="slide" id="slide-<topic>">` in
`web/index.html` with prefixed ids, and one more entry in `nanoverse.main`'s
slide vector. The bundle script doesn't change — there is only one bundle.

For a "group in water" slide most of that work is already done: `geometry.cljs`
declares real coordinates plus what the molecule can do (`:acceptors`,
`:donors`, `:hydrophobic`) and calls `solvent/hydrate`; `babylon_core.cljs`
calls `scene/init`, `scene/state-group!`, `scene/controls!` and reports
whatever counts that slide cares about. Both files stay short, and the
chemistry stays the part you read.

### Babylon specifics to know

- **`Color3`/`Color4` take 0–1 float components**, not hex strings or 0–255 —
  convert once with a small `hex->rgb01` helper rather than hand-computing
  decimals.
- **Updating a dynamic tube's path** must follow Babylon's documented instance
  pattern: mutate the *existing* path `Vector3`s in place with `.copyFrom`, set
  `opts.instance` to the existing mesh, then call `MeshBuilder.CreateTube` again
  with that same `opts` object. Replacing the path array wholesale, or mutating
  a plain `{x,y,z}` object instead of a real `Vector3`, does not update the
  render.
- **A moving group of meshes should share one `TransformNode` parent** whose
  `.position` is updated per frame, with children kept at fixed local offsets —
  cheaper than moving each mesh individually, and it's what makes a rigid
  sub-molecule move as one unit.
- **`GlowLayer` glows any mesh whose material has a non-black `emissiveColor`**,
  with no exclusion list needed if only the meshes meant to glow ever get one
  set.

### Squint gotchas

- **`dotimes` does not give each iteration its own binding.** It compiles to a
  single `let i = 0` hoisted *outside* the loop, so any closure created inside
  the body captures the shared variable and sees its **final** value once the
  loop finishes. Registering event handlers in a `dotimes` therefore wires every
  one of them to the last index — an off-by-N that only shows up when the
  handler fires, never at build time. Pass the index through a **function
  parameter** (a real fresh binding per call) when a closure needs to keep it.
  This bit the deck's slide-dot handlers; `nanoverse.deck/bind-dot!` is the fix.
- **`repeatedly` is lazy, and that silently reorders a seeded PRNG.** Written
  inline as a map value, `(vec (repeatedly 3 #(... (rng) ...)))` can be
  realized *after* the draws written below it in the same literal — so which
  numbers a record gets depends on when something first reads the field, not
  on the source order. It reproduces perfectly on reload and still isn't what
  the code appears to say. Bind every draw in an explicit `let`, in order, when
  the stream has to be deterministic (`nanoverse.solvent/make-water` does, and
  says why). Bit-compare against the previous build after touching anything
  that draws.
- Squint's maps/vectors are **plain JS objects with a copy-on-write `assoc`/
  `conj` layered on top, not real persistent data structures** — correctness
  is fine (originals are never mutated, `=` does structural equality) but
  there's no structural sharing, real hashing, or transients. Immaterial for a
  small, frequently-recomputed scene state; would matter for heavy collection
  reuse at scale.
- A plain Squint map literal (`{:x 1}`) already **is** a real JS object with a
  dot-accessible `x` property — no `#js` wrapping needed to read it from JS.
  `#js` is still the right tool for *constructing* an options object or a
  library-facing value (e.g. a `Vector3`-ready triple), since a map built for
  other Clojure code to consume isn't guaranteed to keep that shape.

### Verification

**Headless screenshot checks must not combine `--headless=new` with
`--disable-gpu`** — Babylon needs a real WebGL context, and that flag
combination is known to leave WebGL non-functional under headless Chromium.
Omit `--disable-gpu` (software/SwiftShader WebGL still initializes) and always
check the actual screenshot pixels, not just a clean console log — a script
exception thrown before the first render call produces both zero console
errors *and* a fully blank canvas.

## Conventions

- Name things for what they teach, and keep code organized so the structure
  itself is part of the explanation (the `geometry`/`babylon_core` split above
  is an example: the chemistry is meant to be readable on its own).
- Explain the biology alongside the result: what the colors mean, and what the
  user should be looking for.
