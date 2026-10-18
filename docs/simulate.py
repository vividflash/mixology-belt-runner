"""
Mastering Mixology strategy simulator. My brain hurts.

  python simulate.py                    compare the main strategies
  python simulate.py --search           try every combination and rank them
  python simulate.py --timing perfect   fastest consistent times instead of typical ones
  python simulate.py --uniform          every potion type equally likely in orders
"""
import argparse
import random
import statistics

TICK = 0.6

XP = {"MMM": 190, "AAA": 190, "LLL": 190, "MMA": 215, "MML": 240,
      "AAM": 265, "ALA": 290, "MLL": 315, "ALL": 340, "MAL": 365}
TYPES = list(XP)
TRIPLES = {"MMM", "AAA", "LLL"}
BY_XP = sorted((t for t in TYPES if t not in TRIPLES), key=XP.get)
RESIN = {t: 20 if t in TRIPLES else 60 if t == "MAL" else 30 for t in TYPES}
RESIN_BONUS = {1: 1.0, 2: 1.2, 3: 1.4}
STATIONS = ["ALEMBIC", "RETORT", "AGITATOR"]
STATION_XP = 14.4
SLOTS = 25

# Seen across 795 recorded orders, in TYPES order.
ORDER_WEIGHTS = [69, 100, 102, 76, 76, 71, 81, 81, 82, 57]

# Ticks, from three recorded sessions with breaks taken out: typical = mean, perfect = 10th percentile.
TIMINGS = {
    "typical": {"first_make": 11.8, "make": 9.3, "to_station": 4.3, "ALEMBIC": 10.2, "RETORT": 8.0,
                "AGITATOR": 7.4, "next_load": 1.1, "switch": 2.9, "to_belt": 4.5, "hand_in": 1.1},
    "perfect": {"first_make": 11, "make": 8, "to_station": 2, "ALEMBIC": 10, "RETORT": 8,
                "AGITATOR": 5, "next_load": 1, "switch": 2, "to_belt": 4, "hand_in": 1},
}
DIGWEED_PICKUP = 8


class Strategy:
    def __init__(self, name, stock, make_from_xp, restock_at, mixalot_rule=False):
        self.name = name
        self.stock = BY_XP[-stock:] if stock else []
        self.make_from_xp = make_from_xp  # unfilled orders worth at least this get made before handing in
        self.restock_at = restock_at      # percent of stock missing before a full restock
        self.mixalot_rule = mixalot_rule  # a showing Mixalot order makes every unfilled order worth making


def simulate(s, seed, hours, timing, weights):
    rng = random.Random(seed)
    weed_rng = random.Random(seed + 1_000_003)
    tm = TIMINGS[timing]
    plan = [(p, st) for st in STATIONS for p in s.stock]
    held, boosted = {}, {}
    t = xp = resin = 0.0
    at_belt = False
    deposits_to_digweed = weed_rng.randint(4, 14)

    def board():
        return [(rng.choices(TYPES, weights)[0] if weights else rng.choice(TYPES), rng.choice(STATIONS))
                for _ in range(3)]

    def count(c):
        return held.get(c, 0) + boosted.get(c, 0)

    orders = board()
    while t < hours * 3600 / TICK:
        left = {c: count(c) for c in orders}
        fills, unfilled = [], []
        for c in orders:
            if left[c] > 0:
                left[c] -= 1
                fills.append(c)
            else:
                unfilled.append(c)
        if s.mixalot_rule and any(p == "MAL" for p, _ in orders):
            wanted = unfilled
        else:
            wanted = [c for c in unfilled if XP[c[0]] >= s.make_from_xp]

        if fills and not wanted:
            t += tm["hand_in"] if at_belt else tm["to_belt"]
            at_belt = True
            for c in fills:
                double = 2 if boosted.get(c) else 1
                (boosted if double == 2 else held)[c] -= 1
                xp += double * XP[c[0]]
                resin += double * RESIN[c[0]] * RESIN_BONUS[len(fills)]
            orders = board()
            deposits_to_digweed -= 1
            if deposits_to_digweed == 0:
                deposits_to_digweed = weed_rng.randint(4, 14)
                options = [c for c, n in held.items() if n]
                if options:
                    best = max(options, key=lambda c: XP[c[0]])
                    held[best] -= 1
                    boosted[best] = boosted.get(best, 0) + 1
                    t += DIGWEED_PICKUP
            continue

        need = wanted or [max(unfilled, key=lambda c: XP[c[0]])]
        batch = []
        missing = [c for c in plan if count(c) == 0]
        if plan and len(missing) * 100 >= s.restock_at * len(plan):
            batch = list(missing)
        for c in need:
            if count(c) + batch.count(c) < need.count(c):
                batch.append(c)
        room = SLOTS - sum(held.values()) - sum(boosted.values())
        batch = sorted(batch, key=lambda c: STATIONS.index(c[1]))[:room]
        if not batch:
            orders = board()
            continue

        t += tm["first_make"] + tm["make"] * (len(batch) - 1) + tm["to_station"]
        last = None
        for p, st in batch:
            t += tm[st] + (tm["next_load"] if st == last else tm["switch"] if last else 0)
            last = st
            held[(p, st)] = held.get((p, st), 0) + 1
            xp += STATION_XP
        at_belt = False

    hrs = t * TICK / 3600
    return xp / hrs, resin / hrs


def score(s, runs, hours, timing, weights):
    results = [simulate(s, seed, hours, timing, weights) for seed in range(runs)]
    return statistics.mean(r[0] for r in results), statistics.mean(r[1] for r in results)


MAIN = [
    Strategy("Focus XP: 5 best stocked, hand in on any fill", 5, 999, 10),
    Strategy("Balanced: 7 stocked, make ALL/MAL orders first", 7, 340, 10),
    Strategy("Old rules: 7 stocked, make non-triples, Mixalot rule, restock 50%", 7, 191, 50, True),
    Strategy("Old rules but always restock", 7, 191, 1, True),
    Strategy("Old rules but top up only", 7, 191, 100, True),
    Strategy("No stock: make every order's potions", 0, 0, 100),
    Strategy("No stock: make every order's potions, skip triples", 0, 191, 100),
]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--timing", choices=TIMINGS, default="typical")
    ap.add_argument("--uniform", action="store_true")
    ap.add_argument("--search", action="store_true")
    ap.add_argument("--runs", type=int, default=100)
    ap.add_argument("--hours", type=float, default=2)
    args = ap.parse_args()
    weights = None if args.uniform else ORDER_WEIGHTS

    if args.search:
        rules = {0: "fill all 3", 191: "skip triples", 240: "make MML+", 290: "make ALA+",
                 340: "make ALL/MAL", 365: "make MAL only", 999: "any fill"}
        found = []
        for stock in range(1, 8):
            for make_from, rule in rules.items():
                for restock in (1, 10, 25, 50, 75, 100):
                    s = Strategy("stock %d, %s, restock %d%%" % (stock, rule, restock), stock, make_from, restock)
                    found.append((score(s, args.runs, args.hours, args.timing, weights), s.name))
        found.sort(reverse=True)
        print("%8s %8s  %s" % ("XP/h", "resin/h", "strategy"))
        for (x, r), name in found[:20]:
            print("%8.0f %8.0f  %s" % (x, r, name))
        return

    print("%8s %8s  %s" % ("XP/h", "resin/h", "strategy"))
    for s in MAIN:
        x, r = score(s, args.runs, args.hours, args.timing, weights)
        print("%8.0f %8.0f  %s" % (x, r, s.name))


if __name__ == "__main__":
    main()
