# GRAMMAR.md

The expression language. Every ambiguity is resolved here, once, permanently.

Changing anything in this file invalidates stored user documents. It requires a
human decision and a document migration.

---

## 1. Precedence table

Lowest to highest. The Pratt parser's binding powers derive directly from this.

| Level | Operators | Associativity |
|---|---|---|
| 1 | `to`, `in`, `as` (unit conversion) | left |
| 2 | `+` `-` | left |
| 3 | `*` `/` `mod` | left |
| 4 | implicit multiplication | left |
| 5 | unary `-` `+` | prefix |
| 6 | `^` | **right** |
| 7 | postfix `%` `!` `°` | postfix |
| 8 | function call, grouping | — |

Note that unary minus sits **below** `^`, giving `-2^2 = -4`. Implicit multiplication
sits at the same effective level as explicit `*` for evaluation purposes, giving
`6/2(1+3) = 12`.

---

## 2. Grammar sketch

```
document   := line*
line       := (label '=')? expr comment?
label      := identifier
comment    := '//' .*

expr       := conversion
conversion := additive (('to'|'in'|'as') unitExpr)?
additive   := multiplicative (('+'|'-') multiplicative)*
multiplicative
           := implicit (('*'|'/'|'mod') implicit)*
implicit   := unary (unary)*            // adjacency, no operator token
unary      := ('-'|'+')* power
power      := postfix ('^' unary)?      // right-assoc via unary on the right
postfix    := primary ('%'|'!'|'°')*
primary    := number unit?
            | identifier
            | lineRef
            | funcCall
            | '(' expr ')'
funcCall   := identifier '(' (expr (ARGSEP expr)*)? ')'
lineRef    := 'line' NUMBER | '@' NUMBER
number     := DIGITS (DECSEP DIGITS)? exponent? suffix?
exponent   := ('e'|'E') ('+'|'-')? DIGITS
```

`DECSEP` and `ARGSEP` are locale-resolved at lex time — see `CONVENTIONS.md` §2.

---

## 3. Ambiguity resolutions

These are the cases that will otherwise be decided differently in every session.

### 3.1 Identifier vs unit — `2m`

**Rule: user-defined variables win over units.**

Resolution order for a bare identifier:
1. Variable defined in this document
2. Built-in constant (`pi`, `e`, `phi`)
3. Registered function name
4. Unit symbol
5. Otherwise → `UnknownIdentifier` with a nearest-match suggestion

So if the user wrote `m = 5` earlier, `2m` is `10`. If not, `2m` is two metres.
When a variable shadows a unit, the editor shows a subtle hint on that line.

### 3.2 Magnitude suffix vs implicit multiplication — `2k`

A suffix binds only when **all** hold:
- it immediately follows a numeric literal with no whitespace
- it is in the active locale's suffix set (`CONVENTIONS.md` §7)
- no variable of that name is in scope

`2k` → `2000`. But after `k = 3`, `2k` → `6`.

### 3.3 Conversion keyword vs unit — `5 in 3`

`to`, `in`, and `as` are **reserved as operators** and cannot be used as variable names.
`in` as the inch unit is only recognised when it appears in unit position — that is,
directly after a numeric literal (`5 in` → five inches) rather than between two
complete expressions (`5 cm in mm` → conversion).

If a user types `5 in 3`, the parser reports `Syntax` on `3`, since `3` is not a
valid unit expression.

### 3.4 Scientific notation — `1e5`

`e` immediately following digits, optionally signed, followed by digits, is an
exponent. `1e5` → `100000`.

Euler's number is written `e` standing alone, or as `exp(1)`. `2e` (digit then bare
`e` with no following digits) is implicit multiplication → `2 × 2.71828…`.

### 3.5 Function call without parentheses — `sin 30`

**Allowed** for single-argument built-in functions. Binding power sits just above
implicit multiplication, so `sin 30 + 1` parses as `sin(30) + 1`, and `sin 2x` parses
as `sin(2x)`.

Not allowed for multi-argument functions or user formulas — those always need
parentheses.

### 3.6 Percent

Percent is contextual, matching calculator convention rather than strict mathematics:

- Additive position: `100 + 10%` → `100 + (100 × 0.10)` = `110`
- Multiplicative position: `100 * 10%` → `100 × 0.10` = `10`
- Standalone: `10%` → `0.1`
- `X% of Y` → `Y × X/100`

### 3.7 Line references

`line3` or `@3` refer to the **stable line id**, not the display ordinal. Inserting a
line above does not repoint an existing reference; the editor rewrites the displayed
token while the underlying id is unchanged.

Circular references produce `CircularReference` with the full cycle, and every line in
the cycle is marked — not just the last one detected.

### 3.8 Unit arithmetic

- Addition and subtraction require compatible dimensions. Otherwise `UnitMismatch`.
  The result takes the left operand's unit: `5 km + 300 m` → `5.3 km`.
- Multiplication and division compose dimensions: `2 h * 60 km/h` → `120 km`.
- Comparison requires compatible dimensions.
- A dimensionless value combines freely with any quantity.
- Temperature is special: `°C` and `°F` are affine, not linear. Addition of two
  absolute temperatures is rejected; adding a delta is allowed. Implement `TempDelta`
  as a distinct unit kind.

---

## 4. Reserved words

Cannot be used as variable or formula names:

```
to  in  as  of  mod  and  or  not  true  false  line  if  else
```

Built-in constants `pi`, `e`, `phi` can be shadowed by user variables, but the editor
warns.

There is no `inf` and no `nan`. A `Quantity` holds a `BigDecimal`, which has no
non-finite values, and inventing a sentinel for them would put a second number model
into every arithmetic path. What would have produced one is an error instead:
`1/0` is `DivisionByZero`, and a result that leaves the representable range is
`Overflow`. Both are spans the editor can point at, which a silent `inf` would not be.
Decided under P0-09; see the decisions log in `TASKS.md`.

---

## 5. Golden test file format

`core/engine/src/test/resources/golden/*.txt`, one case per line:

```
# comment
locale=en-US
angle=DEG
2+3*4                       | 14
-2^2                        | -4
6/2(1+3)                    | 12
100+10%                     | 110
5 km + 300 m                | 5.3 km
1/0                         | !DivisionByZero
sni(1)                      | !UnknownIdentifier@0..2
angle=RAD
sin(pi/6)                   | 0.5
```

- `|` separates input from expectation.
- `!` prefixes an expected error type; `@start..end` asserts the span.
- `locale=` sets the locale for all following lines until changed.
- `angle=` sets the angle mode — `DEG`, `RAD` or `GRAD` — the same way, and defaults to
  `DEG`. A file that never mentions it reads in DEG, the app's own default
  (`docs/CONVENTIONS.md` §4).

**Never edit an existing golden line to make a test pass.** A changed expectation is
a behaviour change and needs a human decision. Add new lines freely.

---

## 6. Fuzzing contract

`EngineFuzzTest` generates random strings — including well-formed-looking garbage,
deeply nested parentheses, huge exponents, and mixed-locale separators — and asserts
that `evaluate()` always returns a `Result`, never throws, and never hangs
(1 second timeout per input).

A parser that survives arbitrary input is the foundation everything else rests on.
If a change breaks fuzzing, the change is wrong.
