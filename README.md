# Mixology Belt Runner

A visual guide for getting the highest xp/h you can out of the Mastering Mixology minigame in OSRS. It
builds on the "Prioritise experience per hour" strategy on the
[OSRS Wiki](https://oldschool.runescape.wiki/w/Mastering_Mixology/Strategies#Prioritise_experience_per_hour),
then tweaks it using a load of simulated hours based on my recorded playing.

# A note for those new to the minigame

If you're under level 81, or you've not got all the Mastering Mixology rewards yet, I don't recommend
this plugin. There's another one called
[Mastering Mixology Extended](https://runelite.net/plugin-hub/show/mastering-mixology-extended) that I
highly recommend. Use that until you've got all the rewards or hit 81.

Mastering Mixology has some incredible Herblore rewards that you should absolutely take advantage of.
A big mechanic of the minigame is that the more orders you fill in one go, the more of its currency
(resin) it gives you. This plugin mostly ignores that for the sake of faster xp.

Want more points for rewards? Use Mastering Mixology Extended.
Want faster xp? Use this plugin.

# So how does this plugin work?

I gotta admit, I'm a big fan of those 'braindead' style plugins. You know the ones, where you can switch
your brain off entirely and just click exactly where it tells you to? I'm only human. This plugin does
that. It highlights practically every click you need to make: which levers to pull and in what order,
which station to use next, when to hit the conveyor belt and when to go grab digweed. It also tracks your
inventory, so it knows what potions you've got, which station each one went through, what you've
used and what you've put digweed on.

It also stops a few of the classic mistakes. Wrong lever, wrong station, or clicking the belt when
nothing on it can be filled (which throws away a potion for half xp) all just get ignored. You can turn
each of those off in the settings if they annoy you, or if you have a problem with that.

# What do I need to get started?

Have a completely empty inventory other than your stack of all 3 pastes. Head inside the game area and
the plugin will take it from there.

At Focus XP pace you'll burn through roughly 3000 Aga and 3000 Lye an hour, and about half that in Mox.
The machine holds up to 3000 of each, so a full machine plus 3000 of each in your inventory is a good
hour and a half or so. The plugin doesn't tell you when to top the machine up, so keep an eye on it.

Remember, this runs best at level 81, when you can start making Mixalot. Before then the xp is even more
dire. Here's to hoping Jagex lower that requirement one day.

## Settings

| Setting | What it does |
|---|---|
| Mode | Focus XP or Balanced (Focus XP by default, see the FAQ) |
| Window style | Next step, To-do list or Full grid |
| Restock at | When nothing can be handed in, restock everything once this much of your stock is gone. Below that, just make the one potion the order needs (10% by default). Restocking early is quicker, because every trip costs the same walk however many potions you make |
| Hold digweed | Keep digweed for an ordered Mixalot (off by default) |
| Station guard | Ignore clicks on an empty station that the next potion isn't meant for (on by default) |
| Lever guard | Ignore pulls on any lever except the next one the potion needs (on by default) |
| Belt lock | Ignore belt clicks when nothing can be handed in, and spare clicks in the same tick (on by default) |
| Path | Where to draw the walking path to your next click: floor, minimap, both or off (both by default) |
| Colours | Highlight (station and vessel), Bonus click colour (green by default), Belt open, Digweed |

# FAQ's

## Why 81?

81 is when you unlock Mixalot (MAL). It's the best potion in the game at 365 xp a pop, and the whole
strategy is built around keeping it stocked. Below 81 the plugin still works and just leaves out the
potions you can't make yet, but your xp/h takes a real hit. It'll warn you about that until you make
your first potion.

## The strategy, in Layman's terms.

1. Keep a stock of finished potions in your inventory: one of each of the five best potions (AAM, ALA,
   MLL, ALL and MAL) for each of the three stations. That's 15 potions.
2. Spam the conveyor belt whenever any order on it matches something you're holding. Don't bother
   waiting to fill all three. As soon as you hand anything in you get three brand new orders, which is
   way quicker than making all three each time.
3. When nothing on the belt matches, make the most valuable order and hand that in.
4. As soon as your stock has gaps, make the missing potions, all of them in one go. Every trip to the
   levers is the same walk, so making more per single trip is quicker.
5. Run your potions through the stations in blocks (all the Alembic ones, then the Retort ones, then
   the Agitator ones) so you're not running back and forth.
6. Digweed doubles a potion's xp. Pop it on a Mixalot whenever you can.

At a steady pace that came out at around 88k xp/h in my own sessions, compared to about 63k doing it
the old way.

## What's Balanced mode?

Same idea, but it stocks all seven non-triple potions and it'll make you fill an Anti-leech lotion
(ALL) or Mixalot order before you hit the belt. You lose a few percent of xp, but you get a bit more
resin and it comes in evenly across all three types. Handy if you still want some points on the side.

## Can I use it with Mastering Mixology or Mastering Mixology Extended?

Not at the same time. Belt Runner pauses itself while either of them is on, and lets you know in chat.
Turn the other one off and it picks back up by itself.

## I had potions in my inventory when I turned it on and it won't start?

It needs to know which station every potion went through, and a finished potion doesn't tell you that.
So it only starts tracking from an inventory with no Mixology potions in it. Hand them in or destroy
them and you're good to go. If you relog with the same inventory it remembers where you were.

## I'm a massive nerd and want to know more about the strategy you've adapted

No worries, nerd! Honestly, it was a pretty messy process with a ton of trial and error. Long and short:
I built a recorder into a dev version of the plugin that logged every click, potion and order while I
played, pulled the real timings out of that, and fed them into a simulator that played thousands of
hours of every strategy I could think of. Before trusting it I checked it against my own sessions. It
guessed 89k xp/h and I actually got 88k, so close enough.

If you want the full writeup, I asked my buddy Claude to put it all together, and you can read all about it inside the github repo, it's in the docs folder. It's got more tables than I know what to do with, so good luck
with that. You'll need it. The Python simulator is in there too, if you fancy running it yourself.

# Credits

Built on code from Hexagon's Mastering Mixology (BSD-2-Clause): the potion and station tables, object
ids and the lab highlighting started there.

The icon's conveyor belt is
[`conveyor_belt` from Google's Material Symbols](https://fonts.google.com/icons?selected=Material+Symbols+Rounded:conveyor_belt)
(Apache-2.0).

# License

BSD-2-Clause. See [LICENSE](LICENSE).
