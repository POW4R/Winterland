# ❄ Winterland — Christmas plugin for Purpur/Paper 26.2

A festive Christmas dimension plus holiday features for the whole server.

## Build & install
```
.\gradlew build
```
Copy `build\libs\Winterland-1.0.0.jar` into the server's `plugins` folder and **fully restart** (no `/reload`).
The first start generates the `winterland` world, which can take a little while.

## What's in it

**Winterland dimension**: an endless Christmas Eve. It's always snowing and always midnight, both configurable.
- Rolling snowy hills, frozen lakes and snow-capped mountains (snowy plains / taiga / grove / frozen peaks biomes)
- Decorated Christmas trees with glowing baubles and a star on top, sometimes with gift chests underneath
- Giant candy canes, living Snow Golems and rare gingerbread houses (with a gift chest inside)
- Ores, so it's a real place to play, not just a showroom

**Christmas in the overworld too**: newly generated chunks in your normal world(s) randomly get Christmas trees, candy canes, snowmen and the occasional gingerbread house. They're fairly common in snowy biomes and rare everywhere else, so expect a decorated tree in a desert or a snowman in a jungle. Their chests are gift chests too. Already-explored terrain is never touched. You can tune the frequency, limit it to December, or pick which worlds get decorations under `overworld-decorations` in the config.

**Advancements**: Winterland has its own tab in the vanilla advancements screen (press `L`), with 17 advancements: travel to Winterland, open presents, visit Santa, build a Snow Golem, win snowball fights, defeat the Grinch, finish the advent calendar and more.

**Gift chests**: every chest generated in Winterland fills with presents and goodies the first time anyone opens it. Chests placed by players never do.

**Santa Claus**: an admin places him with `/xmas santa`, in Winterland, at spawn, or anywhere. Right-click him once per day for a present. If you've been naughty, you get coal instead.

**The Naughty List**: killing villagers, tamed pets, golems, axolotls, allays or other players puts you on it for 24h. Naughty players get coal from Santa and have a 50% chance of coal from presents.

**Naughty Elves & The Grinch**: zombies in Winterland sometimes spawn as elves (baby zombies in green and red) that drop presents and gingerbread. Very rarely, **The Grinch** spawns: a mini-boss with 40 hearts holding a stolen present. Kill him for 3–5 presents and the Grinch's Heart. Right-click the heart and it "grows three sizes" (+6 hearts for 10 minutes).

**Advent Calendar** (`/advent`): 24 doors in December, one per day, with a big reward on door 24. Players can catch up on missed doors (configurable).

**Snowball fights**: snowballs knock players back, chill them briefly and count hits. `/xmas top` shows the leaderboard.

**Items & recipes**

| Item | How to get it |
|---|---|
| ❄ Snow Globe | Glass on top, glass–diamond–glass, 3 snow blocks. Right-click to travel to Winterland and back |
| Gingerbread Cookie ×4 | 2 wheat + cocoa beans + sugar (shapeless). Gives Jump Boost II and saturation |
| Hot Cocoa | glass bottle + cocoa beans + sugar + milk bucket. Gives Speed, Regeneration and thaws you out |
| Santa Hat | `_ _ W / R R R / R _ R` (W = white wool, R = red wool) |
| 🎁 Present | Santa, gift chests, elves, the Grinch, the advent calendar. Right-click to unwrap |

## Commands
| Command | Permission |
|---|---|
| `/xmas tp`, `/xmas leave` | `winterland.tp` (everyone) |
| `/advent` | `winterland.advent` (everyone) |
| `/xmas top` | everyone |
| `/xmas give <item> [player] [amount]` | `winterland.admin` (op) |
| `/xmas santa` / `/xmas santa remove` | `winterland.admin` |
| `/xmas reload` | `winterland.admin` |

## Testing before December
Set `advent.test-day: 5` in `config.yml`, then `/xmas reload`, and the calendar acts as if it's December 5th.
Set it back to `0` when you're done.
