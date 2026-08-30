# nanoverse

A learning project. The user is studying **molecular biology** — chemical bonding,
molecular interactions, structure — and learns by *seeing* things, built
interactively in **Blender** through the **Blender MCP** using the
**Molecular Nodes** addon.

## What "done" looks like here

The goal is understanding, not renders. A deliverable is good when it makes a
chemical idea visible and manipulable, and when the user can change one thing and
watch the consequence.

- Prefer **real structural data** (fetch from the PDB) over invented geometry.
  Chemistry that is made up teaches nothing.
- Drive appearance from **per-atom attributes** so the picture is derived from the
  data, not hand-painted.
- Leave a **knob** the user can turn (a Value node, a socket) rather than a
  hard-coded look.
- **State the limits of the model.** A nearest-atom charge proxy is not a
  Poisson–Boltzmann electrostatic surface; a heavy-atom N/O highlight is not true
  H-bond geometry. Say so — a wrong mental model is worse than no picture.

## Environment

- **Blender 5.2.1 LTS**, driven via `mcp__blender__execute_blender_code`.
- Molecular Nodes is installed as an extension. **Import path is
  `bl_ext.blender_org.molecularnodes`** — plain `import molecularnodes` fails.
- Verify the connection with `get_addon_status` / `get_scene_info` before assuming
  Blender is reachable. If the MCP tools are absent, they are not registered in the
  session; MCP servers only load at session start.

## Working method

Run code in **small steps and verify each one**. The MCP gives a real interpreter
inside a live Blender — use it to *check*, not just to act.

- **Introspect instead of recalling.** The MN Python API has been rewritten; older
  API knowledge (`mn.io.fetch`, etc.) is wrong. Read the live objects:
  `dir()`, `inspect.signature()`, `inspect.getsource()`.
- **Discover enum values by triggering the error.** Assigning an invalid string to a
  menu socket raises an exception that lists the valid options.
- **Verify against evaluated geometry**, not the source mesh. What renders is
  `obj.evaluated_get(bpy.context.evaluated_depsgraph_get()).data`. Geometry nodes
  can add, drop, or change the domain of attributes; the source mesh will lie to you.
- **Read attribute values with numpy** (`foreach_get`) to confirm ranges and sign
  conventions before building a color ramp on top of them.
- **Screenshot after visual changes.** `get_viewport_screenshot` is the ground truth.

### Diagnosing a wrong-looking render

When part of a molecule is colored wrong, bisect rather than guess:

1. Set one ramp stop or base color to a **glaring diagnostic color** (pure green).
   Which parts change tells you which sub-geometry uses which path.
2. Check whether the attribute survives to the **evaluated** geometry at all.
3. Check the **numeric value** reaching the shader, and the ramp's response to it
   (`color_ramp.evaluate(x)` evaluates a ramp directly in Python).

## Molecular Nodes essentials

```python
import bl_ext.blender_org.molecularnodes as mn

mol = (
    mn.Molecule.fetch("1UBQ")          # cached; also .load() for local files
      .add_style(style=mn.StyleBallAndStick(),
                 color=None,           # None = don't touch the Color attribute
                 material=my_material) # accepts a raw bpy.types.Material
)
obj = mol.object
```

Styles: `StyleSpheres`, `StyleSticks`, `StyleBallAndStick`, `StyleCartoon`,
`StyleRibbon`, `StyleSurface`. Selections via `mn.MoleculeSelector()`.
Ready-made materials in `mn.material` (`AmbientOcclusion`, `TransparentOutline`, …).

The material is **not** in an object material slot — it lives in a `Material` socket
on the style node inside the object's `MolecularNodes` geometry-nodes modifier.

### Per-atom attributes (on the POINT domain)

Element/identity: `atomic_number`, `atom_name`, `atom_id`, `res_id`, `res_name`,
`chain_id`, `entity_id`
Physicochemical: `charge` (partial), `lipophobicity`, `mass`, `vdw_radii`,
`b_factor`, `occupancy`
Booleans: `is_backbone`, `is_side_chain`, `is_alpha_carbon`, `is_solvent`,
`is_hetero`, `is_peptide`, `is_nucleic`, `is_carb`
Other: `sec_struct`, `Color`

Read these in a shader with an **Attribute node** (`attribute_type='GEOMETRY'`),
taking the `Fac` output for scalars.

## Gotchas

These have each cost real debugging time.

**`Sphere Geometry = Instance` breaks attribute-driven materials.**
Ball-and-stick defaults to instanced icospheres, and *instances carry no per-atom
attributes* — the Attribute node reads 0 and every sphere renders as the ramp's
zero-position color, while the (realized) sticks color correctly. Set the style's
`Sphere Geometry` socket to `"Mesh"` or `"Point"`.

**Surfaces carry no per-atom attributes.**
`StyleSurface` bakes only a `Color`. To drive a surface from `charge`,
`lipophobicity`, etc., transfer them yourself inside the object's GN tree:
`Position → Sample Nearest` (source = the `Atoms` geometry) → `Sample Index` per
attribute → `Store Named Attribute` on the surface. Optionally `Blur Attribute` to
smooth. Note the transfer samples the *nearest atom*, which over-represents exposed
O/N and skews charge maps negative — use residue-level values for charge patches.

**Attributes are per-mesh, not per-PDB-code.**
Two `Molecule.fetch()` calls of the same structure produce two independent meshes.
A custom attribute must be written to *each* object's mesh.

**Color ramp stops must sit at midpoints, not on the data values.**
Mapping `atomic_number/20` puts carbon at exactly 0.30; float error drops it into
the previous bucket. Place `CONSTANT` stops *between* element values (5.5/20,
6.5/20, …) so rounding cannot matter.

**`lipophobicity` is signed like lipo*philicity*.**
High = greasy/hydrophobic (carbon ≈ +0.08), low/negative = polar (N/O ≈ −0.5).
The name reads backwards. Always confirm a sign convention numerically before
building a ramp on it.

**`res_name_num` collides across protonation variants.**
ASP/ASH, GLU/GLH, LYS/LYN, CYS/CYM/CYX, HIS/HID/HIE/HIP share integer codes in
`assets.data.residues`. Inverting that dict lets the neutral variant silently
overwrite the charged one. Build lookups *from the canonical names outward*:
```python
num2x = {residues[nm]["res_name_num"]: v for nm, v in MY_TABLE.items()}
```
Sanity-check the result against known composition (e.g. ubiquitin: 11 positive,
11 negative, net ≈ 0, pI 6.8).

**X-ray structures usually have no hydrogens.**
So H-bonding must be inferred from N/O heavy atoms, and per-residue sums of partial
charge will not reach formal charge (the H⁺ contributions are missing). Use a
residue formal-charge table instead of summing.

**`mn.session.get_entity()` does not take an object.** Chain from `Molecule.fetch()`
and keep the wrapper, or rebuild it.

## Look and lighting

- **Set `scene.view_settings.view_transform = 'Standard'`.** Blender defaults to
  AgX, which desaturates saturated data colors — actively harmful when color *is*
  the information.
- Render engine enum in Blender 5.x is `'BLENDER_EEVEE'` (there is no
  `BLENDER_EEVEE_NEXT`).
- **MN works at 0.01 scale** — a small protein is ~0.3 Blender units. Lights must be
  scaled to match; area lights of a few watts at ~0.7 BU, not the usual hundreds.
  Blown-out white is the normal first symptom.
- Use `bpy.ops.view3d.view_selected()` under a `temp_override` with a `VIEW_3D` area
  and its `WINDOW` region to frame objects.

## Conventions

- Don't save the .blend or delete user objects without asking. Removing objects this
  session created is fine.
- Name things for what they teach (`MN Chem Lens`), and label/frame shader nodes —
  the node tree is part of the explanation, meant to be read.
- Explain the biology alongside the result: what the colors mean, and what the user
  should be looking for.
