(() => {
  // node_modules/squint-cljs/src/squint/core.js
  function __toFn(x) {
    if (x == null || typeof x === "function") return x;
    if (typeof x === "string") return (coll, d) => get(coll, x, d);
    switch (typeConst(x)) {
      case MAP_TYPE:
      case ARRAY_TYPE:
      case OBJECT_TYPE:
      case SET_TYPE:
        return (k, d) => get(x, k, d);
      case INSTANCE_TYPE:
        if (x[ILookup__lookup] !== void 0) return (k, d) => get(x, k, d);
    }
    return x;
  }
  function isMapLike(m) {
    return m != null && typeof m === "object" && (m.constructor === Object || m instanceof Map || m[TYPE_TAG] === MAP_TYPE);
  }
  function _PLUS_(...xs) {
    return xs.reduce((x, y) => x + y, 0);
  }
  function validateArrayKeys(o, k, kvs) {
    let len = o.length;
    for (let i = 0; i < kvs.length + 2; i += 2) {
      const key = i === 0 ? k : kvs[i - 2];
      if (!Number.isInteger(key)) {
        throw new Error("Vector's key for assoc must be a number.");
      }
      if (key < 0 || key > len) {
        throw new Error(`Index ${key} out of bounds [0,${len}]`);
      }
      if (key === len) len++;
    }
  }
  function assoc_BANG_(m, k, v, ...kvs) {
    if (arguments.length < 3 || kvs.length % 2 !== 0) {
      throw new Error("Illegal argument: assoc expects an odd number of arguments.");
    }
    switch (typeConst(m)) {
      case MAP_TYPE:
        m.set(k, v);
        for (let i = 0; i < kvs.length; i += 2) {
          m.set(kvs[i], kvs[i + 1]);
        }
        break;
      case ARRAY_TYPE:
        validateArrayKeys(m, k, kvs);
        m[k] = v;
        for (let i = 0; i < kvs.length; i += 2) {
          m[kvs[i]] = kvs[i + 1];
        }
        break;
      case INSTANCE_TYPE:
        if (m[ITransientAssociative__assoc_BANG_] !== void 0) {
          let ret = m[ITransientAssociative__assoc_BANG_](m, k, v);
          for (let i = 0; i < kvs.length; i += 2) {
            ret = ret[ITransientAssociative__assoc_BANG_](ret, kvs[i], kvs[i + 1]);
          }
          return ret;
        }
      // fall through: an instance without -assoc! keeps the object behavior
      case OBJECT_TYPE:
        m[k] = v;
        for (let i = 0; i < kvs.length; i += 2) {
          m[kvs[i]] = kvs[i + 1];
        }
        break;
      default:
        throw new Error(
          `Illegal argument: assoc! expects a Map, Array, or Object as the first argument, but got ${typeof m}.`
        );
    }
    return m;
  }
  function copyMeta(from, to) {
    const f = from?.[IMeta__meta];
    if (f !== void 0) {
      to[IMeta__meta] = f;
      to[IWithMeta__with_meta] = from[IWithMeta__with_meta];
    }
    return to;
  }
  function copy(o) {
    switch (typeConst(o)) {
      case MAP_TYPE:
        return copyMeta(o, new o.constructor(o));
      case SET_TYPE:
        return copyMeta(o, new o.constructor(o));
      case ARRAY_TYPE:
        return copyMeta(o, [...o]);
      case INSTANCE_TYPE:
      case OBJECT_TYPE:
        return copyMeta(o, { ...o });
      case LIST_TYPE:
        return copyMeta(o, new List(...o));
      default:
        throw new Error(`Don't know how to copy object of type ${typeof o}.`);
    }
  }
  function assoc(o, k, v, ...kvs) {
    if (arguments.length < 3 || kvs.length % 2 !== 0) {
      throw new Error("Illegal argument: assoc expects an odd number of arguments.");
    }
    if (o == null) {
      o = {};
    }
    if (!isObj(o) && !Array.isArray(o) && o[IAssociative__assoc] !== void 0) {
      let ret2 = o[IAssociative__assoc](o, k, v);
      for (let i = 0; i < kvs.length; i += 2) {
        ret2 = ret2[IAssociative__assoc](ret2, kvs[i], kvs[i + 1]);
      }
      return ret2;
    }
    const ret = copy(o);
    assoc_BANG_(ret, k, v, ...kvs);
    return ret;
  }
  function seq_to_map_for_destructuring(s) {
    const arr = Array.isArray(s) ? s : [...iterable(s)];
    const n = arr.length;
    if (n < 2) return n ? arr[0] : {};
    const m = {};
    for (let i = 0; i < n; i += 2) {
      if (i === n - 1) for (const [k, v] of iterable(arr[i])) m[k] = v;
      else m[arr[i]] = arr[i + 1];
    }
    return m;
  }
  var MAP_TYPE = 1;
  var ARRAY_TYPE = 2;
  var OBJECT_TYPE = 3;
  var LIST_TYPE = 4;
  var SET_TYPE = 5;
  var LAZY_ITERABLE_TYPE = 6;
  var INSTANCE_TYPE = 7;
  var TYPE_TAG = /* @__PURE__ */ Symbol.for("squint.core/type");
  // @__NO_SIDE_EFFECTS__
  function defclass(c) {
    return c;
  }
  function isObj(coll) {
    return coll.constructor === Object;
  }
  function isVectorArray(x) {
    return Array.isArray(x) && x[TYPE_TAG] !== LIST_TYPE;
  }
  function typeConst(obj) {
    if (obj == null) {
      return void 0;
    }
    if (isObj(obj)) {
      return OBJECT_TYPE;
    }
    if (obj instanceof Map) return MAP_TYPE;
    if (obj instanceof Set) return SET_TYPE;
    const tag = obj[TYPE_TAG];
    if (tag !== void 0) return tag;
    if (isVectorArray(obj)) return ARRAY_TYPE;
    if (typeof obj === "object") return INSTANCE_TYPE;
    return void 0;
  }
  function comp(...fs) {
    fs = fs.map(__toFn);
    if (fs.length === 0) {
      return identity;
    } else if (fs.length === 1) {
      return fs[0];
    }
    const [f, ...more] = fs.slice().reverse();
    return function(...args) {
      let x = f(...args);
      for (const g of more) {
        x = g(x);
      }
      return x;
    };
  }
  function conj_BANG_set(o, rest) {
    for (const x of rest) {
      o.add(x);
    }
    return o;
  }
  function conj_BANG_(...xs) {
    const n = xs.length;
    if (n === 0) {
      return vector();
    }
    if (n === 1) {
      return xs[0];
    }
    let o = xs[0];
    if (o === null || o === void 0) {
      o = [];
    }
    if (n === 2) {
      switch (typeConst(o)) {
        case ARRAY_TYPE:
          o.push(xs[1]);
          return o;
        case SET_TYPE:
          o.add(xs[1]);
          return o;
      }
    }
    const rest = xs.slice(1);
    switch (typeConst(o)) {
      case SET_TYPE:
        conj_BANG_set(o, rest);
        break;
      case LIST_TYPE:
        o.unshift(...rest.reverse());
        break;
      case ARRAY_TYPE:
        o.push(...rest);
        break;
      case MAP_TYPE:
        for (const x of rest) {
          if (isVectorArray(x)) {
            asMapEntry(x);
            o.set(x[0], x[1]);
          } else for (const kv of mapEntriesOf(x)) o.set(kv[0], kv[1]);
        }
        break;
      case INSTANCE_TYPE:
        if (o[ITransientCollection__conj_BANG_] !== void 0) {
          let acc = o[ITransientCollection__conj_BANG_](o, rest[0]);
          for (let i = 1; i < rest.length; i++) acc = conj_BANG_(acc, rest[i]);
          return acc;
        }
      // fall through: an instance without -conj! keeps the object behavior
      case OBJECT_TYPE:
        for (const x of rest) {
          if (isVectorArray(x)) {
            asMapEntry(x);
            o[x[0]] = x[1];
          } else for (const kv of mapEntriesOf(x)) o[kv[0]] = kv[1];
        }
        break;
      default:
        throw new Error(
          "Illegal argument: conj! expects a Set, Array, List, Map, or Object as the first argument."
        );
    }
    return o;
  }
  function* mapEntriesOf(x) {
    if (isMapLike(x)) {
      yield* iterable(x);
      return;
    }
    for (const kv of iterable(x)) {
      if (!isVectorArray(kv)) {
        throw new Error("conj on a map takes map entries or seqables of map entries");
      }
      yield kv;
    }
  }
  function asMapEntry(x) {
    if (x.length < 2) {
      throw new Error("Vector arg to map conj must be a pair");
    }
    return x;
  }
  function inc(n) {
    return n + 1;
  }
  function nth(coll, idx, orElse) {
    if (typeof idx !== "number") {
      throw new Error("Index argument to nth must be a number");
    }
    const hasDefault = arguments.length > 2;
    if (coll == null) return hasDefault ? orElse : null;
    if (Array.isArray(coll)) {
      if (idx >= 0 && idx < coll.length) {
        return coll[idx];
      }
    } else if (coll[IIndexed__nth] !== void 0) {
      return hasDefault ? coll[IIndexed__nth](coll, idx, orElse) : coll[IIndexed__nth](coll, idx);
    } else if (idx >= 0) {
      const next = chunkCursor(coll);
      let base = 0;
      let ch;
      while ((ch = next()) !== null) {
        if (idx < base + ch.length) return ch[idx - base];
        base += ch.length;
      }
    }
    if (hasDefault) return orElse;
    throw new Error("Index out of bounds: " + idx);
  }
  function get(coll, key, otherwise = void 0) {
    if (coll == null) {
      return otherwise;
    }
    let v;
    if (isObj(coll)) {
      v = coll[key];
      if (v === void 0) {
        return otherwise;
      } else {
        return v;
      }
    }
    let g;
    switch (typeConst(coll)) {
      case SET_TYPE:
        if (coll.has(key)) v = key;
        break;
      case MAP_TYPE:
        v = coll.get(key);
        break;
      case ARRAY_TYPE:
        v = coll[key];
        break;
      default:
        if (coll[ILookup__lookup] !== void 0) {
          v = coll[ILookup__lookup](coll, key, otherwise);
          return v === void 0 ? otherwise : v;
        }
        g = coll["get"];
        if (typeof g === "function") {
          try {
            v = coll.get(key);
            break;
          } catch (e) {
          }
        }
        v = coll[key];
        break;
    }
    return v !== void 0 ? v : otherwise;
  }
  function sequential_QMARK_(x) {
    return Array.isArray(x) || x?.[TYPE_TAG] === LAZY_ITERABLE_TYPE || x != null && x[IVector.__sym] !== void 0;
  }
  var MAP_ENTRY = /* @__PURE__ */ Symbol.for("squint.core/map-entry");
  function tagMapEntry(e) {
    e[MAP_ENTRY] = true;
    return e;
  }
  function iterable(x) {
    if (x === null || x === void 0) {
      return [];
    }
    if (x[Symbol.iterator]) {
      return x;
    }
    if (x[ISeqable__seq] !== void 0) return iterable(x[ISeqable__seq](x));
    if (isObj(x)) return Object.entries(x).map(tagMapEntry);
    throw new TypeError(`${x} is not iterable`);
  }
  var IIterable = /* @__PURE__ */ Symbol.for("squint.core/IIterable");
  function _iterator(coll) {
    return coll[Symbol.iterator]();
  }
  var es6_iterator = _iterator;
  var REDUCED_DEREF = (self) => self.value;
  var Reduced = class {
    value;
    constructor(x) {
      this.value = x;
      this[IDeref__deref] = REDUCED_DEREF;
    }
  };
  function reduced(x) {
    return new Reduced(x);
  }
  function reduced_QMARK_(x) {
    return x instanceof Reduced;
  }
  function reduce(f, arg1, arg2) {
    f = __toFn(f);
    const hasInit = arguments.length !== 2;
    const coll = hasInit ? arg2 : arg1;
    let val = hasInit ? arg1 : void 0;
    if (Array.isArray(coll)) {
      let i2 = 0;
      if (!hasInit) {
        if (coll.length === 0) return f();
        val = coll[0];
        i2 = 1;
      }
      if (val instanceof Reduced) return val.value;
      for (; i2 < coll.length; i2++) {
        val = f(val, coll[i2]);
        if (val instanceof Reduced) return val.value;
      }
      return val;
    }
    const next = chunkCursor(coll);
    let ch = next();
    let i = 0;
    if (!hasInit) {
      if (ch === null) return f();
      val = ch[0];
      i = 1;
    }
    if (val instanceof Reduced) return val.value;
    while (ch !== null) {
      for (; i < ch.length; i++) {
        val = f(val, ch[i]);
        if (val instanceof Reduced) return val.value;
      }
      ch = next();
      i = 0;
    }
    return val;
  }
  var CHUNK_SIZE = 32;
  var LazyIterable = /* @__PURE__ */ defclass(
    class LazyIterable2 {
      constructor(step) {
        this[TYPE_TAG] = LAZY_ITERABLE_TYPE;
        this[IIterable] = true;
        this.step = step;
        this.realized = false;
        this.chunk = null;
        this._rest = null;
      }
      force() {
        if (!this.realized) {
          this.realized = true;
          const r = this.step();
          this.step = null;
          if (r !== null && r !== void 0) {
            this.chunk = r[0];
            this._rest = new LazyIterable2(r[1]);
          }
        }
        return this;
      }
      [Symbol.iterator]() {
        let cell = this;
        let i = 0;
        return {
          next() {
            for (; ; ) {
              cell.force();
              const ch = cell.chunk;
              if (ch === null) return { value: void 0, done: true };
              if (i < ch.length) return { value: ch[i++], done: false };
              cell = cell._rest;
              i = 0;
            }
          },
          [Symbol.iterator]() {
            return this;
          }
        };
      }
      // Mirrors Array.prototype.indexOf so lazy seqs support (.indexOf coll x):
      // reference equality, returns -1 when absent. Unlike cljs.core, not by value.
      indexOf(x, fromIndex = 0) {
        let i = 0;
        for (const v of this) {
          if (i >= fromIndex && v === x) return i;
          i++;
        }
        return -1;
      }
    }
  );
  function unchunkedSteps(iter) {
    const step = () => {
      const r = iter.next();
      return r.done ? null : [[r.value], step];
    };
    return step;
  }
  function lazy(f) {
    return new LazyIterable(unchunkedSteps(f()));
  }
  function lazyIter(coll, gen) {
    const it = es6_iterator(iterable(coll));
    return lazy(() => gen(it));
  }
  function chunkCells(coll) {
    if (coll instanceof LazyIterable) return coll;
    if (Array.isArray(coll)) {
      const step = (pos) => () => {
        if (pos >= coll.length) return null;
        const end = Math.min(pos + CHUNK_SIZE, coll.length);
        return [coll.slice(pos, end), step(end)];
      };
      return new LazyIterable(step(0));
    }
    return new LazyIterable(unchunkedSteps(es6_iterator(iterable(coll))));
  }
  function chunkCursor(coll) {
    if (coll instanceof LazyIterable) {
      let cell = coll;
      return () => {
        if (cell === null) return null;
        cell.force();
        const ch = cell.chunk;
        cell = ch === null ? null : cell._rest;
        return ch;
      };
    }
    const it = es6_iterator(iterable(coll));
    return () => {
      const b = [];
      for (let i = 0; i < CHUNK_SIZE; i++) {
        const r = it.next();
        if (r.done) break;
        b.push(r.value);
      }
      return b.length === 0 ? null : b;
    };
  }
  function mapChunks(coll, xf) {
    const src = chunkCells(coll);
    const step = (cell, base) => () => {
      let c = cell;
      let b = base;
      for (; ; ) {
        c.force();
        const ch = c.chunk;
        if (ch === null) return null;
        const out = xf(ch, b);
        const rest = c._rest;
        b += ch.length;
        if (out.length !== 0) return [out, step(rest, b)];
        c = rest;
      }
    };
    return new LazyIterable(step(src, 0));
  }
  function map(f, ...colls) {
    f = __toFn(f);
    switch (colls.length) {
      case 0:
        return (rf) => {
          return (...args) => {
            switch (args.length) {
              case 0: {
                return rf();
              }
              case 1: {
                return rf(args[0]);
              }
              case 2: {
                return rf(args[0], f(args[1]));
              }
              default: {
                return rf(args[0], f(...args.slice(1)));
              }
            }
          };
        };
      case 1:
        return mapChunks(colls[0], (ch) => {
          const out = new Array(ch.length);
          for (let i = 0; i < ch.length; i++) out[i] = f(ch[i]);
          return out;
        });
      default: {
        const iters = colls.map((coll) => es6_iterator(iterable(coll)));
        return lazy(function* () {
          while (true) {
            const args = [];
            for (const i of iters) {
              const nextVal = i.next();
              if (nextVal.done) {
                return;
              }
              args.push(nextVal.value);
            }
            yield f(...args);
          }
        });
      }
    }
  }
  function transducer(step) {
    return (rf) => {
      const s = step(rf);
      return (...args) => args.length === 0 ? rf() : args.length === 1 ? rf(args[0]) : s(args[0], args[1]);
    };
  }
  function map_indexed1(f) {
    return transducer((rf) => {
      let i = -1;
      return (r, x) => rf(r, f(++i, x));
    });
  }
  function map_indexed(f, coll) {
    f = __toFn(f);
    if (arguments.length === 1) {
      return map_indexed1(f);
    }
    return mapChunks(coll, (ch, base) => {
      const out = new Array(ch.length);
      for (let i = 0; i < ch.length; i++) out[i] = f(base + i, ch[i]);
      return out;
    });
  }
  function str(...xs) {
    return xs.join("");
  }
  function name(x) {
    if (typeof x === "string") {
      const i = x.indexOf("/");
      return i >= 1 ? x.slice(i + 1) : x;
    }
    throw new Error("Doesn't support name: " + typeof x);
  }
  function not(expr) {
    return !truth_(expr);
  }
  var IATOM_SYM = /* @__PURE__ */ Symbol.for("squint.core/IAtom");
  var IDEREF_SYM = /* @__PURE__ */ Symbol.for("squint.core/IDeref");
  var IDeref__deref = /* @__PURE__ */ Symbol.for("squint.core/-deref");
  function _deref(o) {
    if (o != null && o[IDeref__deref] !== void 0) return o[IDeref__deref](o);
    return nilImpl(_deref, "IDeref.-deref", o)(o);
  }
  var ISeqable__seq = /* @__PURE__ */ Symbol.for("squint.core/-seq");
  var ILookup__lookup = /* @__PURE__ */ Symbol.for("squint.core/-lookup");
  var IAssociative__assoc = /* @__PURE__ */ Symbol.for("squint.core/-assoc");
  var ICounted__count = /* @__PURE__ */ Symbol.for("squint.core/-count");
  var IKVReduce__kv_reduce = /* @__PURE__ */ Symbol.for("squint.core/-kv-reduce");
  var ICollection__conj = /* @__PURE__ */ Symbol.for("squint.core/-conj");
  var ITransientCollection__conj_BANG_ = /* @__PURE__ */ Symbol.for("squint.core/-conj!");
  var ITransientAssociative__assoc_BANG_ = /* @__PURE__ */ Symbol.for("squint.core/-assoc!");
  var IMeta__meta = /* @__PURE__ */ Symbol.for("squint.core/-meta");
  var IWithMeta__with_meta = /* @__PURE__ */ Symbol.for("squint.core/-with-meta");
  var M3_C1 = 3432918353 | 0;
  var M3_C2 = 461845907 | 0;
  var IIndexed__nth = /* @__PURE__ */ Symbol.for("squint.core/-nth");
  var IVector = { __sym: /* @__PURE__ */ Symbol.for("squint.core/IVector") };
  function nilImpl(dispatchFn, protoMethod, o) {
    const f = dispatchFn[null];
    if (f === void 0) throw missing_protocol(protoMethod, o);
    return f;
  }
  var IReset = { __sym: /* @__PURE__ */ Symbol.for("squint.core/IReset") };
  var IReset__reset_BANG_ = /* @__PURE__ */ Symbol.for("squint.core/-reset!");
  function _reset_BANG_(o, v) {
    if (o != null && o[IReset__reset_BANG_] !== void 0) return o[IReset__reset_BANG_](o, v);
    return nilImpl(_reset_BANG_, "IReset.-reset!", o)(o, v);
  }
  var ISwap = { __sym: /* @__PURE__ */ Symbol.for("squint.core/ISwap") };
  var ISwap__swap_BANG_ = /* @__PURE__ */ Symbol.for("squint.core/-swap!");
  var IWatchable = { __sym: /* @__PURE__ */ Symbol.for("squint.core/IWatchable") };
  var IWatchable__add_watch = /* @__PURE__ */ Symbol.for("squint.core/-add-watch");
  var IWatchable__remove_watch = /* @__PURE__ */ Symbol.for("squint.core/-remove-watch");
  var IWatchable__notify_watches = /* @__PURE__ */ Symbol.for("squint.core/-notify-watches");
  var ATOM_DEREF = (self) => self.val;
  var ATOM_RESET = (self, x) => {
    if (self._validator && !truth_(self._validator(x))) {
      throw new Error("Validator rejected reference state");
    }
    const old_val = self.val;
    self.val = x;
    if (self._hasWatches) {
      for (const [k, f] of Object.entries(self._watches)) f(k, self, old_val, x);
    }
    return x;
  };
  var ATOM_SWAP = function(self, f, a, b, xs) {
    switch (arguments.length) {
      case 2:
        return ATOM_RESET(self, f(self.val));
      case 3:
        return ATOM_RESET(self, f(self.val, a));
      case 4:
        return ATOM_RESET(self, f(self.val, a, b));
      default:
        return ATOM_RESET(self, f(self.val, a, b, ...xs));
    }
  };
  var ATOM_ADD_WATCH = (self, k, f) => {
    self._watches[k] = f;
    self._hasWatches = true;
  };
  var ATOM_REMOVE_WATCH = (self, k) => {
    delete self._watches[k];
  };
  var ATOM_NOTIFY = (self, oldv, newv) => {
    for (const [k, f] of Object.entries(self._watches)) f(k, self, oldv, newv);
  };
  var Atom = class {
    constructor(init) {
      this.val = init;
      this._watches = {};
      this._hasWatches = false;
      this[IATOM_SYM] = true;
      this[IDEREF_SYM] = true;
      this[IDeref__deref] = ATOM_DEREF;
      this[IReset.__sym] = true;
      this[IReset__reset_BANG_] = ATOM_RESET;
      this[ISwap.__sym] = true;
      this[ISwap__swap_BANG_] = ATOM_SWAP;
      this[IWatchable.__sym] = true;
      this[IWatchable__add_watch] = ATOM_ADD_WATCH;
      this[IWatchable__remove_watch] = ATOM_REMOVE_WATCH;
      this[IWatchable__notify_watches] = ATOM_NOTIFY;
    }
  };
  function atom(init, ...opts) {
    const a = new Atom(init);
    for (let i = 0; i < opts.length; i += 2) {
      if (opts[i] === "meta") {
        const mv = opts[i + 1];
        a[IMeta__meta] = () => mv;
      } else if (opts[i] === "validator") a._validator = opts[i + 1];
    }
    return a;
  }
  function missing_protocol(proto, obj) {
    let ty;
    if (obj === null) ty = "null";
    else if (obj === void 0) ty = "undefined";
    else if (Array.isArray(obj)) ty = "array";
    else if (typeof obj === "object" && obj.constructor && obj.constructor !== Object) {
      ty = obj.constructor.name;
    } else ty = typeof obj;
    return new Error(
      `No protocol method ${proto} defined for type ${ty}: ${obj ?? ""}`
    );
  }
  function deref(ref) {
    if (ref?.[IDeref__deref] !== void 0) return ref[IDeref__deref](ref);
    return nilImpl(_deref, "IDeref.-deref", ref)(ref);
  }
  function reset_BANG_(atm, v) {
    if (atm?.[IReset__reset_BANG_] !== void 0) return atm[IReset__reset_BANG_](atm, v);
    return nilImpl(_reset_BANG_, "IReset.-reset!", atm)(atm, v);
  }
  function swap_BANG_(atm, f, ...args) {
    f = __toFn(f);
    if (atm?.[ISwap__swap_BANG_] !== void 0) {
      switch (args.length) {
        case 0:
          return atm[ISwap__swap_BANG_](atm, f);
        case 1:
          return atm[ISwap__swap_BANG_](atm, f, args[0]);
        case 2:
          return atm[ISwap__swap_BANG_](atm, f, args[0], args[1]);
        default:
          return atm[ISwap__swap_BANG_](atm, f, args[0], args[1], args.slice(2));
      }
    }
    const v = f(deref(atm), ...args);
    reset_BANG_(atm, v);
    return v;
  }
  function range(begin, end, step) {
    let b = begin, e = end, s = step;
    if (end === void 0) {
      b = 0;
      e = begin;
    }
    const start = b || 0;
    s = step ?? 1;
    const ascending = s >= 0;
    const more = (i) => e === void 0 || ascending && i < e || !ascending && e < i;
    const mkStep = (from) => () => {
      if (!more(from)) return null;
      const out = [];
      let i = from;
      while (out.length < CHUNK_SIZE && more(i)) {
        out.push(i);
        i += s;
      }
      return [out, mkStep(i)];
    };
    return new LazyIterable(mkStep(start));
  }
  function vector(...args) {
    return args;
  }
  function vector_QMARK_(x) {
    if (x == null) return false;
    return isVectorArray(x) || x[IVector.__sym] !== void 0;
  }
  function mapv(...args) {
    if (args.length === 2) {
      const [_f, coll] = args;
      const f = __toFn(_f);
      const iter = iterable(coll);
      if (Array.isArray(iter)) {
        const ret = new Array(iter.length);
        for (var i = 0; i < iter.length; i++) {
          ret[i] = f(iter[i]);
        }
        return ret;
      } else {
        const ret = [];
        const next = chunkCursor(iter);
        let ch;
        while ((ch = next()) !== null) {
          for (let i2 = 0; i2 < ch.length; i2++) ret.push(f(ch[i2]));
        }
        return ret;
      }
    }
    return [...map(...args)];
  }
  function pushAll(out, from) {
    if (from instanceof LazyIterable) {
      let cell = from;
      for (; ; ) {
        cell.force();
        const ch = cell.chunk;
        if (ch === null) return out;
        Array.prototype.push.apply(out, ch);
        cell = cell._rest;
      }
    }
    for (const x of iterable(from)) out.push(x);
    return out;
  }
  function vec(x) {
    if (isVectorArray(x)) return x;
    if (x != null && x[IVector.__sym] !== void 0) return x;
    return pushAll([], x);
  }
  var List = class extends Array {
    constructor(...args) {
      super();
      this[TYPE_TAG] = LIST_TYPE;
      this.push(...args);
    }
  };
  var CONCAT_DONE = {};
  function concat1(colls) {
    const isArr = Array.isArray(colls);
    const arr = isArr ? colls.slice() : null;
    const collIter = isArr ? null : es6_iterator(iterable(colls));
    let idx = 0;
    const nextColl = () => {
      if (isArr) {
        if (idx >= arr.length) return CONCAT_DONE;
        const c = arr[idx];
        arr[idx] = null;
        idx++;
        return c;
      }
      const r = collIter.next();
      return r.done ? CONCAT_DONE : r.value;
    };
    const step = (src) => () => {
      for (; ; ) {
        if (src !== null) {
          if (src instanceof LazyIterable) {
            src.force();
            if (src.chunk !== null) return [src.chunk, step(src._rest)];
            src = null;
          } else {
            const a = src.a;
            const pos = src.pos;
            if (pos < a.length) {
              const end = Math.min(pos + CHUNK_SIZE, a.length);
              return [a.slice(pos, end), step({ a, pos: end })];
            }
            src = null;
          }
        }
        const nc = nextColl();
        if (nc === CONCAT_DONE) return null;
        src = nc instanceof LazyIterable ? nc : Array.isArray(nc) ? { a: nc, pos: 0 } : chunkCells(nc);
      }
    };
    return new LazyIterable(step(null));
  }
  function mapcat(f, ...colls) {
    if (colls.length === 0) {
      return comp(map(f), cat);
    }
    return concat1(map(f, ...colls));
  }
  function identity(x) {
    return x;
  }
  function into(...args) {
    let to, xform, from, c, rf;
    switch (args.length) {
      case 0:
        return [];
      case 1:
        return args[0];
      case 2:
        to = args[0] ?? [];
        if (isVectorArray(to)) {
          return pushAll(copy(to), args[1]);
        }
        if (to[ICollection__conj] !== void 0) {
          return reduce(
            (acc, x) => acc != null && acc[ICollection__conj] !== void 0 ? acc[ICollection__conj](acc, x) : conj_BANG_(acc, x),
            to,
            args[1]
          );
        }
        return reduce(conj_BANG_, copy(to), args[1]);
      case 3:
        to = args[0];
        xform = args[1];
        from = args[2];
        c = to != null && to[ICollection__conj] !== void 0 ? to : copy(to);
        rf = (coll, v) => {
          if (v === void 0) {
            return coll;
          }
          return coll != null && coll[ICollection__conj] !== void 0 ? coll[ICollection__conj](coll, v) : conj_BANG_(coll, v);
        };
        return transduce(xform, rf, c, from);
      default:
        throw TypeError(`Invalid arity call of into: ${args.length}`);
    }
  }
  function ensure_reduced(x) {
    if (reduced_QMARK_(x)) {
      return x;
    } else {
      return reduced(x);
    }
  }
  function take1(n) {
    return transducer((rf) => {
      let na = n;
      return (r, x) => {
        const nn = --na + 1;
        if (nn > 0) r = rf(r, x);
        return nn > 1 ? r : ensure_reduced(r);
      };
    });
  }
  function assertNumber(n) {
    if (typeof n !== "number") {
      throw new Error("Assert failed: (number? n)");
    }
    return n;
  }
  function take(n, coll) {
    assertNumber(n);
    if (arguments.length === 1) {
      return take1(n);
    }
    return lazyIter(coll, function* (it) {
      let i = n - 1;
      for (const x of it) {
        if (i-- >= 0) {
          yield x;
        }
        if (i < 0) {
          return;
        }
      }
    });
  }
  function update(coll, k, f, ...args) {
    f = __toFn(f);
    return assoc(coll, k, f(get(coll, k), ...args));
  }
  function _repeatedly(f) {
    return lazy(function* () {
      while (true) yield f();
    });
  }
  function repeatedly(n, f) {
    if (arguments.length === 1) {
      f = n;
      n = void 0;
    }
    const res = _repeatedly(f);
    if (n !== void 0) {
      if (typeof n !== "number") {
        throw new Error("repeatedly: count must be a number, got: " + str(n));
      }
      return take(n, res);
    } else {
      return res;
    }
  }
  function count(coll) {
    if (!coll) return 0;
    const len = coll.length || coll.size;
    if (typeof len === "number") {
      return len;
    }
    if (coll[ICounted__count] !== void 0) return coll[ICounted__count](coll);
    const next = chunkCursor(coll);
    let ret = 0;
    let ch;
    while ((ch = next()) !== null) ret += ch.length;
    return ret;
  }
  function truth_(x) {
    return x != null && x !== false;
  }
  function subs(s, start, end) {
    return s.substring(start, end);
  }
  function keys(obj) {
    if (obj == null) return null;
    const t = typeConst(obj);
    switch (t) {
      case INSTANCE_TYPE:
        if (obj[IKVReduce__kv_reduce] !== void 0) {
          const ks = obj[IKVReduce__kv_reduce](obj, (acc, k, _) => (acc.push(k), acc), []);
          if (ks.length) return ks;
          return;
        }
      // fall through
      case OBJECT_TYPE: {
        const ks = Object.keys(obj);
        if (ks.length) return ks;
        return;
      }
      case MAP_TYPE:
        if (obj.size) return Array.from(obj.keys());
        return;
    }
    for (const _ of iterable(obj)) {
      throw new TypeError(obj + " is not a map");
    }
    return null;
  }
  function vals(obj) {
    if (obj == null) return null;
    const t = typeConst(obj);
    switch (t) {
      case INSTANCE_TYPE:
        if (obj[IKVReduce__kv_reduce] !== void 0) {
          const vs = obj[IKVReduce__kv_reduce](obj, (acc, _, v) => (acc.push(v), acc), []);
          if (vs.length) return vs;
          return;
        }
      // fall through
      case OBJECT_TYPE: {
        const vs = Object.values(obj);
        if (vs.length) return vs;
        return;
      }
      case MAP_TYPE:
        if (obj.size) return Array.from(obj.values());
        return;
    }
    for (const _ of iterable(obj)) {
      throw new TypeError(obj + " is not a map");
    }
    return null;
  }
  function keyword(arg1, arg2) {
    if (arg2 !== void 0) {
      return (arg1 != null ? arg1 + "/" : "") + arg2;
    }
    return arg1;
  }
  function transduce(xform, ...args) {
    switch (args.length) {
      case 2: {
        const f = args[0];
        const coll = args[1];
        return transduce(xform, f, f(), coll);
      }
      default: {
        let f = args[0];
        const init = args[1];
        const coll = args[2];
        f = xform(f);
        const ret = reduce(f, init, coll);
        return f(ret);
      }
    }
  }
  function preserving_reduced(rf) {
    return (a1, a2) => {
      const ret = rf(a1, a2);
      if (reduced_QMARK_(ret)) {
        return reduced(ret);
      } else return ret;
    };
  }
  function cat(rf) {
    const rrf = preserving_reduced(rf);
    return (...args) => {
      switch (args.length) {
        case 0:
          return rf();
        case 1:
          return rf(args[0]);
        case 2:
          return reduce(rrf, args[0], args[1]);
      }
    };
  }

  // out/hydration_console/geometry.mjs
  var OH_LEN = 0.9572;
  var HOH_ANGLE = 104.5 * Math.PI / 180;
  var eth_raw = { "C1": { "x": 7e-3, "y": -0.569, "z": 0 }, "H9": { "x": 1.986, "y": -0.137, "z": 0 }, "C2": { "x": -1.285, "y": 0.25, "z": 0 }, "O": { "x": 1.13, "y": 0.315, "z": 0 }, "H8": { "x": -2.142, "y": -0.424, "z": 0 }, "H6": { "x": -1.317, "y": 0.878, "z": 0.89 }, "H7": { "x": -1.317, "y": 0.878, "z": -0.89 }, "H5": { "x": 0.039, "y": -1.197, "z": -0.89 }, "H4": { "x": 0.039, "y": -1.197, "z": 0.89 } };
  var add = function(a, b) {
    return { "x": get(a, "x") + get(b, "x"), "y": get(a, "y") + get(b, "y"), "z": get(a, "z") + get(b, "z") };
  };
  var sub = function(a, b) {
    return { "x": get(a, "x") - get(b, "x"), "y": get(a, "y") - get(b, "y"), "z": get(a, "z") - get(b, "z") };
  };
  var scale = function(a, s) {
    return { "x": get(a, "x") * s, "y": get(a, "y") * s, "z": get(a, "z") * s };
  };
  var vdot = function(a, b) {
    return get(a, "x") * get(b, "x") + get(a, "y") * get(b, "y") + get(a, "z") * get(b, "z");
  };
  var vcross = function(a, b) {
    return { "x": get(a, "y") * get(b, "z") - get(a, "z") * get(b, "y"), "y": get(a, "z") * get(b, "x") - get(a, "x") * get(b, "z"), "z": get(a, "x") * get(b, "y") - get(a, "y") * get(b, "x") };
  };
  var vlen = function(a) {
    return Math.sqrt(vdot(a, a));
  };
  var vnorm = function(a) {
    const l_1 = (() => {
      const or__23663__auto___2 = vlen(a);
      if (truth_(or__23663__auto___2)) {
        return or__23663__auto___2;
      } else {
        return 1;
      }
      ;
    })();
    return { "x": get(a, "x") / l_1, "y": get(a, "y") / l_1, "z": get(a, "z") / l_1 };
  };
  var eth_keys = keys(eth_raw);
  var eth = (() => {
    const centroid_1 = scale(reduce(add, { "x": 0, "y": 0, "z": 0 }, vals(eth_raw)), 1 / count(eth_raw));
    return into({}, lazy((function* () {
      for (let G__2 of iterable(eth_keys)) {
        const k_3 = G__2;
        yield [k_3, sub(get(eth_raw, k_3), centroid_1)];
      }
      return null;
    })));
  })();
  var eth_bonds = [["C1", "C2"], ["C1", "O"], ["C1", "H4"], ["C1", "H5"], ["C2", "H6"], ["C2", "H7"], ["C2", "H8"], ["O", "H9"]];
  var build_basis = function(away_dir) {
    const e1_1 = scale(away_dir, -1);
    const ref_2 = Math.abs(get(e1_1, "y")) > 0.9 ? { "x": 1, "y": 0, "z": 0 } : { "x": 0, "y": 1, "z": 0 };
    const e2_3 = vnorm(vcross(e1_1, ref_2));
    return { "e1": e1_1, "e2": e2_3 };
  };
  var water_atoms = function(basis) {
    const h1_1 = scale(get(basis, "e1"), OH_LEN);
    const h2_2 = add(scale(get(basis, "e1"), Math.cos(HOH_ANGLE) * OH_LEN), scale(get(basis, "e2"), Math.sin(HOH_ANGLE) * OH_LEN));
    return { "O": { "x": 0, "y": 0, "z": 0 }, "H1": h1_1, "H2": h2_2 };
  };
  var acceptor_dir = vnorm(sub(get(eth, "H9"), get(eth, "O")));
  var water_defs = [{ "name": "donor_1", "away": vnorm({ "x": -0.7, "y": -0.9, "z": 0.5 }), "oo": 3.26 }, { "name": "donor_2", "away": vnorm({ "x": -0.9, "y": 0.6, "z": -0.4 }), "oo": 3.34 }, { "name": "acceptor", "away": acceptor_dir, "oo": 3.06 }, { "name": "tail_1", "away": vnorm({ "x": 0.3, "y": 0.9, "z": 0.8 }), "oo": 3.6 }, { "name": "tail_2", "away": vnorm({ "x": 0.9, "y": 0.7, "z": -0.6 }), "oo": 4.2 }, { "name": "tail_3", "away": vnorm({ "x": -0.2, "y": -0.3, "z": -1 }), "oo": 5 }];
  var mulberry32 = function(seed0) {
    const seed_1 = atom(seed0 | 0);
    return function() {
      swap_BANG_(seed_1, (function(s) {
        return s + 1831565813 | 0;
      }));
      const s_2 = deref(seed_1);
      const t1_3 = Math.imul(s_2 ^ s_2 >>> 15, 1 | s_2);
      const t2_4 = t1_3 + Math.imul(t1_3 ^ t1_3 >>> 7, 61 | t1_3) ^ t1_3;
      return ((t2_4 ^ t2_4 >>> 14) >>> 0) / 4294967296;
    };
  };
  var rng = mulberry32(20260830);
  var waters = vec(map((function(wd) {
    const basis_1 = build_basis(get(wd, "away"));
    const local_2 = water_atoms(basis_1);
    const base_o_3 = add(get(eth, "O"), scale(get(wd, "away"), get(wd, "oo")));
    const wobble_4 = vec(repeatedly(3, (function() {
      return [{ "freq": 0.6 + rng() * 0.5, "phase": rng() * Math.PI * 2, "amp": 0.62 }, { "freq": 1.3 + rng() * 0.9, "phase": rng() * Math.PI * 2, "amp": 0.38 }];
    })));
    return { "name": get(wd, "name"), "basis": basis_1, "local": local_2, "base-o": base_o_3, "wobble": wobble_4 };
  }), water_defs));
  var jitter_for = function(water, t) {
    return into({}, map_indexed((function(i, ax) {
      return [ax, reduce(_PLUS_, map((function(term) {
        return get(term, "amp") * Math.sin(t * get(term, "freq") + get(term, "phase"));
      }), nth(get(water, "wobble"), i)))];
    }), ["x", "y", "z"]));
  };
  var pairs = vec(mapcat((function(i, _w) {
    return [{ "donor": i, "donor-atom": "H1", "acceptor": null }, { "donor": i, "donor-atom": "H2", "acceptor": null }, { "donor": null, "donor-atom": "H9", "acceptor": i }];
  }), range(), waters));
  var DIST_LIMIT = 2.5;
  var COS_LIMIT = -0.5;
  var compute_current = function(w, t, amp) {
    const j_1 = jitter_for(w, t);
    const o_2 = add(get(w, "base-o"), scale(j_1, amp));
    const local_3 = get(w, "local");
    return { "O": o_2, "H1": add(o_2, get(local_3, "H1")), "H2": add(o_2, get(local_3, "H2")) };
  };

  // out/hydration_console/babylon_core.mjs
  var UNITS_PER_A = 0.55;
  var to_vec3 = function(a) {
    return new BABYLON.Vector3(get(a, "x") * UNITS_PER_A, get(a, "y") * UNITS_PER_A, get(a, "z") * UNITS_PER_A);
  };
  var element_of = function(k) {
    return keyword(subs(name(k), 0, 1));
  };
  var hex__GT_rgb01 = function(hex) {
    const h_1 = subs(hex, 1);
    return [parseInt(subs(h_1, 0, 2), 16) / 255, parseInt(subs(h_1, 2, 4), 16) / 255, parseInt(subs(h_1, 4, 6), 16) / 255];
  };
  var canvas = document.getElementById("render-canvas");
  var engine = new BABYLON.Engine(canvas, true);
  var scene = new BABYLON.Scene(engine);
  var vec___11 = hex__GT_rgb01("#0d1219");
  var r2 = nth(vec___11, 0, null);
  var g3 = nth(vec___11, 1, null);
  var b4 = nth(vec___11, 2, null);
  scene.clearColor = new BABYLON.Color4(r2, g3, b4, 1);
  var camera = new BABYLON.ArcRotateCamera("cam", Math.PI / 2 - 0.5, 1.15, 8.5, new BABYLON.Vector3(0, 0, 0), scene);
  camera.attachControl(canvas, true);
  camera.lowerRadiusLimit = 4;
  camera.upperRadiusLimit = 20;
  camera.wheelPrecision = 40;
  var hemi_light = new BABYLON.HemisphericLight("hemi", new BABYLON.Vector3(0, 1, 0.2), scene);
  hemi_light.intensity = 0.75;
  var dir_light = new BABYLON.DirectionalLight("dir", new BABYLON.Vector3(-0.5, -1, -0.3), scene);
  dir_light.intensity = 0.55;
  var glow = new BABYLON.GlowLayer("glow", scene);
  glow.intensity = 0.7;
  var make_material = function(hex, glow_QMARK_) {
    const vec___1_4 = hex__GT_rgb01(hex);
    const r_5 = nth(vec___1_4, 0, null);
    const g_6 = nth(vec___1_4, 1, null);
    const b_7 = nth(vec___1_4, 2, null);
    const m_8 = new BABYLON.StandardMaterial(`mat-${hex ?? ""}`, scene);
    const c_9 = new BABYLON.Color3(r_5, g_6, b_7);
    m_8.diffuseColor = c_9;
    if (truth_(glow_QMARK_)) {
      m_8.emissiveColor = c_9;
    }
    ;
    return m_8;
  };
  var materials = { "C": make_material("#4b4f58", false), "O": make_material("#e2555a", false), "H": make_material("#eef1f4", false), "bond": make_material("#9aa5b1", false), "hbond": make_material("#59e0c9", true) };
  var R = { "C": 0.32, "O": 0.34, "H": 0.2 };
  var BOND_RADIUS = 0.045;
  var HBOND_RADIUS = 0.035;
  var make_sphere = function(name_str, diameter, material, pos) {
    const s_1 = BABYLON.MeshBuilder.CreateSphere(name_str, { "diameter": diameter, "segments": 20 }, scene);
    s_1.position = pos;
    s_1.material = material;
    return s_1;
  };
  var make_bond = function(name_str, a_pos, b_pos) {
    const t_1 = BABYLON.MeshBuilder.CreateTube(name_str, { "path": [a_pos, b_pos], "radius": BOND_RADIUS, "tessellation": 8 }, scene);
    t_1.material = get(materials, "bond");
    return t_1;
  };
  var eth_group = new BABYLON.TransformNode("ethanol", scene);
  for (let G__1 of iterable(eth_keys)) {
    let k5 = G__1;
    const el_2 = element_of(k5);
    const s_3 = make_sphere(`eth-${name(k5)}`, get(R, el_2), get(materials, el_2), to_vec3(get(eth, k5)));
    s_3.parent = eth_group;
  }
  for (let G__1 of iterable(eth_bonds)) {
    let vec___26 = G__1;
    let a7 = nth(vec___26, 0, null);
    let b8 = nth(vec___26, 1, null);
    const t_5 = make_bond(`eth-bond-${name(a7)}-${name(b8)}`, to_vec3(get(eth, a7)), to_vec3(get(eth, b8)));
    t_5.parent = eth_group;
  }
  var anchors = mapv((function(w) {
    const node_1 = new BABYLON.TransformNode(`water-${get(w, "name") ?? ""}`, scene);
    const local_2 = get(w, "local");
    const o_pos_3 = to_vec3(get(local_2, "O"));
    const h1_pos_4 = to_vec3(get(local_2, "H1"));
    const h2_pos_5 = to_vec3(get(local_2, "H2"));
    node_1.position = to_vec3(get(w, "base-o"));
    for (let G__6 of iterable([make_sphere(`${get(w, "name") ?? ""}-O`, get(R, "O"), get(materials, "O"), o_pos_3), make_sphere(`${get(w, "name") ?? ""}-H1`, get(R, "H"), get(materials, "H"), h1_pos_4), make_sphere(`${get(w, "name") ?? ""}-H2`, get(R, "H"), get(materials, "H"), h2_pos_5), make_bond(`${get(w, "name") ?? ""}-b1`, o_pos_3, h1_pos_4), make_bond(`${get(w, "name") ?? ""}-b2`, o_pos_3, h2_pos_5)])) {
      const m_7 = G__6;
      m_7.parent = node_1;
    }
    ;
    return node_1;
  }), waters);
  var tubes = mapv((function(_) {
    const opts_1 = { "path": [new BABYLON.Vector3(0, 0, 0), new BABYLON.Vector3(0, 0, 1e-3)], "radius": HBOND_RADIUS, "tessellation": 8, "updatable": true };
    const mesh_2 = BABYLON.MeshBuilder.CreateTube("hbond", opts_1, scene);
    mesh_2.material = get(materials, "hbond");
    mesh_2.isVisible = false;
    return { "mesh": mesh_2, "opts": opts_1 };
  }), pairs);
  var amp_input = document.getElementById("amp");
  var speed_input = document.getElementById("speed");
  var amp_value_el = document.getElementById("amp-value");
  var speed_value_el = document.getElementById("speed-value");
  var play_toggle = document.getElementById("play-toggle");
  var readout_count = document.getElementById("readout-count");
  var readout_dist = document.getElementById("readout-dist");
  var reduce_motion_QMARK_ = (() => {
    const and__23694__auto___1 = window.matchMedia;
    if (truth_(and__23694__auto___1)) {
      return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    } else {
      return and__23694__auto___1;
    }
    ;
  })();
  var state = atom({ "amplitude": parseFloat(amp_input.value), "speed": parseFloat(speed_input.value), "running": not(reduce_motion_QMARK_), "t-accum": 0, "last-frame": null });
  play_toggle.textContent = truth_(get(deref(state), "running")) ? "Pause" : "Play";
  amp_input.addEventListener("input", (function() {
    const v_1 = parseFloat(amp_input.value);
    swap_BANG_(state, assoc, "amplitude", v_1);
    return amp_value_el.textContent = `${v_1.toFixed(2) ?? ""}${" \xC5"}`;
  }));
  speed_input.addEventListener("input", (function() {
    const v_1 = parseFloat(speed_input.value);
    swap_BANG_(state, assoc, "speed", v_1);
    return speed_value_el.textContent = `${v_1.toFixed(1) ?? ""}${"\xD7"}`;
  }));
  play_toggle.addEventListener("click", (function() {
    swap_BANG_(state, update, "running", not);
    return play_toggle.textContent = truth_(get(deref(state), "running")) ? "Pause" : "Play";
  }));
  window.addEventListener("resize", (function() {
    return engine.resize();
  }));
  var frame = function() {
    const map___1_3 = deref(state);
    const map___1_4 = truth_(sequential_QMARK_(map___1_3)) ? truth_(vector_QMARK_(map___1_3)) ? map___1_3 : seq_to_map_for_destructuring(map___1_3) : map___1_3;
    const running_5 = get(map___1_4, "running");
    const amplitude_6 = get(map___1_4, "amplitude");
    const speed_7 = get(map___1_4, "speed");
    const last_frame_8 = get(map___1_4, "last-frame");
    const t_accum_9 = get(map___1_4, "t-accum");
    const now_10 = performance.now();
    const lf_11 = (() => {
      const or__23663__auto___12 = last_frame_8;
      if (truth_(or__23663__auto___12)) {
        return or__23663__auto___12;
      } else {
        return now_10;
      }
      ;
    })();
    const dt_13 = Math.min((now_10 - lf_11) / 1e3, 0.05);
    const t_accum_SINGLEQUOTE__14 = truth_(running_5) ? t_accum_9 + dt_13 * speed_7 : t_accum_9;
    const current_15 = mapv((function(_PERCENT_1) {
      return compute_current(_PERCENT_1, t_accum_SINGLEQUOTE__14, amplitude_6);
    }), waters);
    const active_count_16 = atom(0);
    const nearest_17 = atom(Infinity);
    swap_BANG_(state, assoc, "last-frame", now_10, "t-accum", t_accum_SINGLEQUOTE__14);
    const n9_18 = count(waters);
    let i_19 = 0;
    for (; i_19 < n9_18; i_19++) {
      nth(anchors, i_19).position.copyFrom(to_vec3(get(nth(current_15, i_19), "O")));
    }
    ;
    const n10_20 = count(pairs);
    let idx_21 = 0;
    for (; idx_21 < n10_20; idx_21++) {
      (() => {
        const p_24 = nth(pairs, idx_21);
        const has_donor_25 = !(get(p_24, "donor") == null);
        const w_idx_26 = has_donor_25 ? get(p_24, "donor") : get(p_24, "acceptor");
        const c_27 = nth(current_15, w_idx_26);
        const H_28 = has_donor_25 ? get(p_24, "donor-atom") === "H1" ? get(c_27, "H1") : get(c_27, "H2") : get(eth, "H9");
        const D_29 = has_donor_25 ? get(c_27, "O") : get(eth, "O");
        const A_30 = has_donor_25 ? get(eth, "O") : get(c_27, "O");
        const dist_vec_31 = sub(A_30, H_28);
        const dist_32 = vlen(dist_vec_31);
        const vec_hd_33 = vnorm(sub(D_29, H_28));
        const vec_ha_34 = vnorm(dist_vec_31);
        const cos_angle_35 = vdot(vec_hd_33, vec_ha_34);
        const active_36 = dist_32 < DIST_LIMIT && cos_angle_35 < COS_LIMIT;
        const map___22_37 = nth(tubes, idx_21);
        const map___22_38 = truth_(sequential_QMARK_(map___22_37)) ? truth_(vector_QMARK_(map___22_37)) ? map___22_37 : seq_to_map_for_destructuring(map___22_37) : map___22_37;
        const mesh_39 = get(map___22_38, "mesh");
        const opts_40 = get(map___22_38, "opts");
        mesh_39.isVisible = active_36;
        if (truth_(active_36)) {
          opts_40.path[0].copyFrom(to_vec3(H_28));
          opts_40.path[1].copyFrom(to_vec3(A_30));
          opts_40.instance = mesh_39;
          BABYLON.MeshBuilder.CreateTube("hbond", opts_40);
          swap_BANG_(active_count_16, inc);
        }
        ;
        if (dist_32 < deref(nearest_17)) {
          return reset_BANG_(nearest_17, dist_32);
        }
        ;
      })();
    }
    ;
    readout_count.textContent = deref(active_count_16);
    readout_count.className = `num${(deref(active_count_16) === 0 ? " zero" : "") ?? ""}`;
    return readout_dist.textContent = truth_(isFinite(deref(nearest_17))) ? `${deref(nearest_17).toFixed(2) ?? ""}${" \xC5"}` : "\u2014";
  };
  engine.runRenderLoop((function() {
    frame();
    return scene.render();
  }));
})();
