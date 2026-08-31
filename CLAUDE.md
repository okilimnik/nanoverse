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
  Chemistry that is made up teaches nothing.
- Drive appearance from **per-atom attributes** so the picture is derived from the
  data, not hand-painted.
- Leave a **knob** the user can turn (a slider, a control) rather than a
  hard-coded look.
- **State the limits of the model.** A distance-only hydrogen-bond proxy is not a
  real electron-density calculation; a hand-tuned sine-wave wobble is not a
  physics simulation. Say so — a wrong mental model is worse than no picture.

## Web visualizations

Standalone browser visualizations live under `web/`, one HTML file per
visualization at the top level. They are written in **ClojureScript**, compiled
with **Squint** (`squint-cljs` — a lightweight CLJS-to-JS compiler that runs on
plain Node, no JVM), and rendered with **Babylon.js** (loaded from cdnjs as a
classic global script exposing `window.BABYLON` — the plain-`<script src>`
build, not an ES module).

### Toolchain

Everything lives in `web/cljs/`:
- `squint.edn` — compiles every `.cljs` file under `src/` in one pass.
- `package.json` — `npm run build` runs `squint compile`, then bundles each
  entry-point namespace with `esbuild --bundle --format=iife` into its own file
  under `dist/`. The output is a single plain script per page — no
  `<script type="module">`, no import maps — so a built page still opens
  directly via `file://`, same as any other static HTML file.
- `src/<topic>/geometry.cljs` — pure chemistry/physics data and math for a
  given visualization, with **zero rendering-engine dependency**.
  `src/<topic>/babylon_core.cljs` `:require`s it and does only the
  Babylon-specific parts (scene/camera/mesh construction, the per-frame render
  loop). Keeping these separate means the same verified chemistry can be
  re-pointed at a different renderer later without touching the math.

To add a new visualization: new `<topic>/geometry.cljs` + `<topic>/babylon_core.cljs`
under `web/cljs/src/`, a new `web/<topic>.html` shell (copy an existing one's
CSS/DOM, point its script tags at Babylon's cdnjs URL and the new bundle), and
one more `esbuild` line in `package.json`'s `build` script.

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
