# The maths behind Belt Runner

*An empirical model of Mastering Mixology, and what it says about the fastest way to train Herblore there.*

*Plugin author's note: I tried to read this and had no idea what any of it was saying. But apparently I figured out some pretty cool shit? Who'da thunk.*

## Abstract

Mastering Mixology rewards Herblore xp per potion handed in, and the "pre-make" strategy (keep a stock
of finished potions, work the conveyor belt) is the accepted fast method. It leaves several decisions
open: which potions to stock, when to hand in, when to restock and what to do with triples, Mixalot and
digweed. We instrumented a development build of the plugin to record every game tick of real play
(three sessions, 93.5 minutes in the lab, 402 potions, 262 hand-ins, 795 orders), measured the
minigame's reward rules, order generation and action timings from that record, and built a discrete
time simulator from the measurements. The simulator predicts real sessions to within about 1%. Searching
the strategy space with it shows that **the hand-in rule dominates every other choice**: handing in
whenever any order can be filled, and never making a potion for an order while something else on the
belt can be handed in, raises the rate from 79.5k to 89.1k xp/h at the recorded pace (+12.1%, 95% CI
±0.1%). The best stock is the five most valuable non-triples, restocked as soon as anything is missing.
Resin per hour is nearly insensitive to strategy; its split across the three types is not, which
motivates a second "Balanced" mode.

## 1. The problem

The game's own description of the minigame, and the wiki's
[xp strategy](https://oldschool.runescape.wiki/w/Mastering_Mixology/Strategies#Prioritise_experience_per_hour),
leave these questions unanswered:

1. **Restock or top up?** When the belt can't be worked, should the player refill every gap in their
   stock, or make only the potions the current orders need?
2. **What to stock?** Seven non-triple potions exist. Are all seven worth a slot?
3. **When to hand in?** The game pays more resin for filling two or three orders at once. Does waiting
   for a fuller hand-in pay off in xp?
4. **Triples, Mixalot, digweed.** Existing guides treat these specially. Should they?

Each question trades time against reward under randomness, which makes intuition unreliable and
makes the answer depend on numbers nobody had measured.

## 2. The minigame as a system

Terms used throughout:

- **Potion**: one of 10 recipes of three pastes, Mox (M), Aga (A) and Lye (L). Seven mix pastes
  (MMA, MML, AAM, ALA, MLL, ALL, MAL); three are **triples** (MMM, AAA, LLL).
- **Station**: Alembic, Retort or Agitator. Every potion must go through one before it can be handed in.
  A finished potion's item does not record which station finished it.
- **Order**: a (potion, station) pair. The **board** shows three orders.
- **Hand-in**: clicking the conveyor belt. Every finished potion that matches an order is handed in at
  once.
- **Cell**: a (potion, station) pair held in stock. A full five-type stock is 15 cells.
- **Trip**: a visit to the levers to make potions, followed by processing them and returning to the belt.

A station always takes the first unprocessed potion in inventory slot order, so the order potions are
made in decides which station each goes through. The plugin tracks every slot for that reason.

## 3. Data collection

A development build of the plugin carried a recorder, never shipped, that wrote to a SQLite database. It
recorded:

| Table | One row per |
|---|---|
| `tick` | game tick: position, current instruction, clicks, vessel contents, stock counts, paste levels, xp |
| `step` | instruction shown: when it appeared, the first click, when it was replaced |
| `make` | potion taken from the vessel: lever pulls, ticks taken, whether it was the one asked for |
| `process` | station visit: load and clear ticks, bonus-click windows and hits, progress setbacks |
| `deposit` / `deposit_item` | hand-in: potions, orders filled, xp and resin gained, failures |
| `board` / `board_order` / `board_stock` | set of orders: the three orders, the stock held when it appeared, what was filled |
| `event` | raw facts (clicks, chat, varbits, digweed) as JSON |

Three sessions were recorded on one account at Herblore 89:

| Session | Rules | Minutes in lab | xp/h | Potions | Hand-ins | Failed hand-ins |
|---|---|---|---|---|---|---|
| 1 | old guide rules, restock at 50% | 14.5 | 63,093 | 59 | 22 | 0 |
| 2 | Focus XP | 36.9 | 88,058 | 166 | 120 | 11 |
| 3 | Focus XP, one belt click per tick | 42.1 | 83,438 (90,225 without breaks) | 177 | 120 | 0 |

Session 3 contains three breaks of more than 10 seconds without a click, 317 ticks in total (7.5%).

## 4. Measured mechanics

### 4.1 Rewards

Hand-in xp is the sum of the potions' recipe xp, doubled for a potion carrying digweed. Resin is a fixed
amount per potion, split evenly across the potion's three paste letters, doubled for digweed, and
multiplied by a bonus that depends on how many orders the hand-in fills:

| Potion | xp | Resin |
|---|---|---|
| MMM, AAA, LLL | 190 | 20 |
| MMA | 215 | 30 |
| MML | 240 | 30 |
| AAM | 265 | 30 |
| ALA | 290 | 30 |
| MLL | 315 | 30 |
| ALL | 340 | 30 |
| MAL | 365 | 60 |

| Orders filled in one hand-in | xp bonus | Resin multiplier |
|---|---|---|
| 1 | none | 1.0 |
| 2 | none | 1.2 |
| 3 | none | 1.4 |

The model reproduces **all 251 successful hand-ins exactly**, both xp and resin. The full-order bonus is
resin only; it never affects xp. A failed hand-in ("You deposit some X, but fail to fulfil an order")
removes one potion and pays half its xp.

Each station visit also pays roughly 14 xp from bonus clicks. Every strategy processes every potion it
makes, so this term barely separates strategies.

### 4.2 Board refresh

Every hand-in that fills at least one order replaces all three orders, **in the same game tick**
(observed delay 0 ticks on every hand-in). Unfilled orders are discarded, not kept. This single fact
drives most of the results: a fresh board costs about one tick.

### 4.3 Order generation

Over 265 boards (795 orders):

| MMM | AAA | LLL | MMA | MML | AAM | ALA | MLL | ALL | MAL |
|---|---|---|---|---|---|---|---|---|---|
| 69 | 100 | 102 | 76 | 76 | 71 | 81 | 81 | 82 | 57 |

- **Potion types are not uniform**: χ² = 20.8 on 9 degrees of freedom, p = 0.014. Mixalot appears about
  28% less often than a uniform share and AAA and LLL about 26% more.
- **Stations are uniform**: 254 / 283 / 258, χ² = 1.86, p = 0.39.
- **Orders do not depend on what the player holds.** Naively, orders landed on held cells less often
  than chance (283 against 308.4 expected, z = −1.85, p = 0.064). Recomputing the expectation with the
  observed type weights removes the effect (283 against 287.9, z = −0.37, p = 0.71). The deficit is
  explained entirely by the stock holding MAL, which is rare, and no triples, which are common.
- **Orders within a board are independent**: 21 boards repeated an order, against 25.9 expected.

The simulator draws types from the observed weights and stations uniformly. Uniform types are kept as a
sensitivity case (section 7.8).

### 4.4 Digweed

Digweed doubles one potion's xp and resin. The game counts hand-ins since the last spawn; 30 spawns came
after 4 to 14 hand-ins (mean 8.6). The counter does not advance while an unused digweed is held, so
digweed should be used promptly rather than saved in the inventory.

### 4.5 Action timings

Durations in game ticks (0.6 s) over all three sessions. Gaps longer than three times the median are
treated as breaks and dropped (at most 3 per measure).

| Measure | n | Mean | SD | Median | 10th pct |
|---|---|---|---|---|---|
| Belt to first potion taken (walk + make) | 76 | 11.83 | 2.17 | 11 | 11 |
| Each further potion | 322 | 9.26 | 3.00 | 8 | 8 |
| Last potion taken to first station load | 80 | 4.28 | 2.21 | 3 | 2 |
| Alembic | 129 | 10.21 | 0.91 | 10 | 10 |
| Retort | 140 | 8.04 | 0.25 | 8 | 8 |
| Agitator | 134 | 7.42 | 2.12 | 7 | 5 |
| Next load, same station | 220 | 1.10 | 0.32 | 1 | 1 |
| Switching station | 103 | 2.86 | 0.82 | 3 | 2 |
| Last station to hand-in | 79 | 4.53 | 0.66 | 4 | 4 |
| Consecutive hand-ins | 183 | 1.11 | 0.33 | 1 | 1 |

Two timing profiles are used: **typical** (means) and **perfect** (10th percentiles, the fastest time
hit consistently). The Agitator's 10th percentile of 5 reflects the double-click technique. Means rather
than medians are deliberate: a median-timed simulator predicted 96k xp/h for a strategy that really ran
at 88k, because the occasional slow action is part of the real cost.

### 4.6 Late clicks land on the next board

Session 2 lost 11 potions to failed hand-ins, every one exactly one tick after the player's last belt
click on a fillable board, onto a board where nothing could be filled. The probability depended on how
many belt clicks fell in that final tick:

| Belt clicks in the final tick | Followed by a failed hand-in |
|---|---|
| 1 | 1 of 9 |
| 2 | 1 of 9 |
| 3 | 3 of 7 |
| 4 | 6 of 6 |

A click made late in a client tick reaches the server after the next server tick has begun. By then the
first click's hand-in has replaced the board, and the late click hands in to the new one. The plugin's
belt lock had checked the click against the board on screen, which was already stale. The fix is to
allow one belt click per tick, since a tick can only hand in once. Session 3 had no failed hand-ins.

## 5. The simulator

[simulate.py](simulate.py) is a discrete time model, standard library only.

### 5.1 Loop

```
draw a board of 3 orders
repeat until the time budget is spent:
    match finished stock against the board
    wanted = unfilled orders the strategy says are worth making first
    if something fills and nothing is wanted:
        hand in (to_belt the first time, hand_in after that), score xp and resin, draw a new board
        count towards digweed; on a spawn, pay the pickup and double the best potion held
    else:
        need = wanted, or the single most valuable unfilled order if nothing fills
        batch = need, plus every missing stock cell if the restock threshold is reached
        trip: first_make + make x (n - 1) + to_station,
              then each potion's station time, next_load within a station, switch between stations
```

Potions in a batch are made and processed station by station (Alembic, Retort, Agitator), as the plugin
does, so a trip visits each station once.

### 5.2 Strategy space

| Parameter | Meaning | Values searched |
|---|---|---|
| Stock size *k* | the *k* highest-xp non-triples, one per station | 0 to 7 |
| Hand-in rule | an unfilled order worth at least *x* xp is made before handing in | fill all 3, skip triples, MML+, ALA+, ALL/MAL, MAL only, any fill |
| Restock threshold | full restock once this share of the stock is missing; below it, top up only | 1% to 100% |
| Mixalot rule | a showing Mixalot order makes every unfilled order wanted (the old guide's rule) | on, off |

"Any fill" means no unfilled order is ever made while something else on the board can be handed in.

### 5.3 Variance reduction

Every strategy is evaluated on the same seeds, and orders and digweed draw from separate random streams.
Strategy A and strategy B therefore face the same sequence of boards, and differences between them are
measured as paired differences (common random numbers). Each comparison below uses 200 to 300 runs of 2
simulated hours, each starting from an empty inventory.

### 5.4 Simplifications

- Timings are deterministic per profile. Real variation is folded into the means.
- Bonus-click xp is a constant 14.4 per potion.
- Hopper refills are ignored. They cost the same per potion under every strategy.
- Digweed is always used on the best potion held, immediately.
- Inventory: 25 free slots (28 minus three paste stacks).

## 6. Validation

| Session | Predicted | Observed |
|---|---|---|
| 1, old rules, from an empty inventory, 14.5 min (first-generation simulator, session 1 timings) | 61.1k (10th to 90th pct 58.1k to 66.5k) | 63.1k |
| 2, Focus XP | 89.1k | 88.1k |
| 3, Focus XP, breaks removed | 89.1k | 90.2k |

The two Focus XP sessions bracket the prediction. The model is good enough to rank strategies; its
absolute numbers carry roughly ±2%, mostly from player pace.

## 7. Results

Typical timing and observed order weights unless stated. ± is a 95% confidence interval.

### 7.1 Main comparison

| Strategy | xp/h | Resin/h |
|---|---|---|
| **Focus XP**: stock 5, any fill, restock at 10% | **89,142 ± 101** | 11,089 ± 22 |
| **Balanced**: stock 7, make ALL/MAL orders first, restock at 10% | 86,761 ± 83 | 11,862 ± 22 |
| Old rules but always restock | 80,865 ± 86 | 11,777 ± 21 |
| Old rules: stock 7, skip triples, Mixalot rule, restock at 50% | 79,524 ± 91 | 11,540 ± 22 |
| Old rules but top up only | 75,639 ± 129 | 10,824 ± 27 |
| No stock, skip triples | 73,222 | 10,137 |
| No stock, make every order | 69,863 | 10,450 |

Paired differences:

| Comparison | xp/h | Resin/h |
|---|---|---|
| Focus XP − old rules | **+9,618 ± 100** (+12.1%) | −451 ± 20 |
| Balanced − old rules | +7,237 ± 86 | +322 ± 15 |
| Focus XP − Balanced | +2,381 ± 79 | −773 ± 16 |
| Old rules, always restock − restock at 50% | +1,341 ± 69 | +237 ± 12 |
| Old rules, top up only − restock at 50% | −3,885 ± 119 | −716 ± 20 |

### 7.2 The hand-in rule

Stock 5, restock at 10%:

| Rule | xp/h | Resin/h |
|---|---|---|
| Fill all 3 before handing in | 70,887 | 10,815 |
| Skip triples | 79,461 | 11,242 |
| Make MML and better | 82,957 | 11,142 |
| Make ALA and better | 87,167 | 11,140 |
| Make ALL and MAL | 88,375 | 11,176 |
| Make MAL only | 88,894 | 11,189 |
| **Any fill** | **89,063** | 11,071 |

The spread, 18k xp/h, is larger than any other parameter's. The reason is the cost asymmetry from 4.2
and 4.5: making a potion for an unfilled order costs about 9 ticks at the levers plus about 9 at a
station, plus a share of the trip's walking, while a fresh board costs 1.1 ticks. At full stock, a fresh
board has at least one fillable order with probability 1 − (1 − 0.468)³ = 0.85, where 0.468 is the
weighted share of orders that land on one of the 15 stocked cells. Handing in and rolling the board
almost always beats making a potion.

### 7.3 Stock size

Any fill, restock at 10%:

| Types stocked | Potions | xp/h | Resin/h |
|---|---|---|---|
| 1 (MAL) | 3 | 75,334 | 8,806 |
| 2 | 6 | 80,921 | 9,514 |
| 3 | 9 | 85,217 | 10,141 |
| 4 | 12 | 87,947 | 10,658 |
| **5** | **15** | **89,063** | 11,071 |
| 6 | 18 | 88,876 | 11,419 |
| 7 | 21 | 87,457 | 11,671 |

More cells raise the fill probability but each extra type is cheaper and costs a full slot set to keep
stocked. The optimum is five (AAM, ALA, MLL, ALL, MAL). Six is within noise of five for xp and gives
slightly more resin.

### 7.4 Restock or top up (the original question)

Focus XP with the threshold varied:

| Restock when missing | xp/h |
|---|---|
| 1% (any cell) | 89,093 ± 120 |
| 10% | 89,063 ± 123 |
| 25% | 88,884 ± 122 |
| 50% | 87,459 ± 139 |
| 75% | 83,865 ± 167 |
| 100% (top up only) | 76,982 ± 205 |

**Restock early.** Every trip pays the same fixed walking cost whether it makes one potion or ten, so
fewer, larger trips win, and a fuller stock fills more boards. Anything up to about 25% is equivalent;
the plugin defaults to 10%. Topping up only is 13.6% slower. The same holds under the old rules (+1.3k
for always restocking, −3.9k for top up only).

### 7.5 The Mixalot rule

Adding the old guide's rule (a showing Mixalot order makes every unfilled order wanted) to Focus XP costs
5.6k xp/h (89,063 to 83,493). The rule exists to complete full orders for resin; for xp it forces trips
for triples and cheap potions that any-fill would skip.

### 7.6 When nothing fills

With nothing fillable, the alternative to making the best order is to hand in anyway and accept a failed
hand-in (lose a potion for half xp) just to roll the board. Simulated with the strategy optimiser
(bootstrapped real timings, uniform orders): making the order gives 89.8k xp/h in Focus XP against 78.4k
for failing on purpose, and 87.4k against 84.2k in Balanced. The lost potion has to be remade later,
which costs more than the trip it avoids.

### 7.7 Digweed placement

Using digweed on the best potion held and saving it for a Mixalot (making one if needed, so spawns keep
running) differed by 0.05% (104,157 against 104,107 xp/h, perfect timing). Placement is irrelevant;
holding it unused is not, because it stops the spawn counter.

### 7.8 Sensitivity

| Strategy | Typical, weighted | Typical, uniform | Perfect, uniform |
|---|---|---|---|
| Focus XP | 89,142 | 90,789 | 104,820 |
| Balanced | 86,761 | 87,533 | 100,961 |
| Old rules | 79,524 | 80,063 | 92,854 |

The ranking is the same under every combination. Rarer Mixalot orders cost each strategy up to 2%.
Perfect play lifts everything by about 15 to 16% without reordering anything.

## 8. Xp against resin

Among strategies within 20% of the best xp rate, resin per hour varies by only about 8% (13.4k to 14.5k
at perfect pace). Filling two
or three orders for the 1.2× and 1.4× resin bonus never repays the potions made to get there, so no
xp-competitive strategy can buy much extra resin.

What strategy does change is the **split**. Focus XP stocks no MMA or MML, the Mox-heavy potions, so
its Mox income falls well below its Aga and Lye income. At perfect pace with uniform orders, the strategy
optimiser measured:

| Strategy | xp/h | Resin/h | Mox / Aga / Lye |
|---|---|---|---|
| Focus XP | 104.2k | 13.4k | 3.4k / 5.0k / 5.0k |
| Stock 6, make ALL/MAL first | 103.0k | 14.1k | 4.2k / 4.7k / 5.2k |
| **Balanced**: stock 7, make ALL/MAL first | 101.0k | 14.4k | 4.8k / 4.8k / 4.8k |

Balanced keeps 97% of the xp and gives an even three-way split, which matters for rewards priced in all
three resins. It is the second mode in the plugin.

## 9. From model to plugin

| Finding | Implementation |
|---|---|
| Any fill, never make while something can be handed in | `Mode.FOCUS_XP`: no unfilled order is worth making |
| Five types | `Mode.FOCUS_XP` stocks 5; `Mode.BALANCED` stocks 7 |
| ALL/MAL first for an even resin split | `Mode.BALANCED`: orders worth 340 xp or more are made first |
| Nothing fills: make the best order | `BeltRules`: `NOTHING_FILLABLE` names the highest-xp unfilled order |
| Restock early | Restock default 10%, range 1% to 100% |
| No Mixalot rule | removed |
| Late clicks | belt lock lets one belt click through per tick |
| Digweed: use promptly | the guide asks for it as soon as the batch is made; a half-made potion is finished first |

## 10. Limitations

- **One player, three sessions.** Timings belong to one player's pace, mouse and network. The ranking is
  robust to a 15% change in pace (7.8), but absolute rates will differ between players.
- **Order weights rest on 795 orders.** Each type count carries a standard error of about ±8. The
  conclusions hold under uniform weights too.
- **Deterministic timings.** The simulator uses means, not distributions. The strategy optimiser, which
  bootstraps real timing samples, reached the same ranking.
- **The first validation used an earlier simulator.** Session 1 predates the current timing set; the two
  Focus XP sessions are the stronger check.
- **Digweed spawns** are modelled as uniform on 4 to 14 hand-ins, matching the observed range and mean.
- **Hopper refills, bank trips and breaks** are outside the model.

## 11. Reproducing

```bash
python simulate.py                    # section 7.1 at typical pace (single runs, no intervals)
python simulate.py --search           # every combination of stock, hand-in rule and restock, best 20
python simulate.py --timing perfect   # 10th-percentile timings
python simulate.py --uniform          # uniform potion types
python simulate.py --runs 300         # more runs per strategy
```

Timings, order weights and reward tables are constants at the top of the file; replace them with your
own measurements to model a different pace.
