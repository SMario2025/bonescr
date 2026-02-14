# BonescraftLand (Paper 1.20.1) — Land + View-Only Chests + Monthly Playtime

## What this plugin does
### Land rules (as requested)
- Players can **ONLY build/break inside their own claimed chunks** (or chunks where they are trusted).
- **Unclaimed chunks are fully protected** → everything already built is safe.
- Players can **open chests/containers**, but **cannot take/move items** unless:
  - they are the **owner/trusted** of that land, OR
  - they have the special permission **bonescraft.land.loot** (your “loot rank”)

### Extra
- **Global build rank**: permission **bonescraft.land.globalbuild** → build anywhere without claiming
- **Admins/Owner/Co-Owner**: permission **bonescraft.land.admin** → bypass everything
- **Monthly playtime tracking**
  - /playtime → shows your playtime this month
  - /playtime top → top 10 this month (admin)
  - playtime is stored in `plugins/BonescraftLand/playtime.yml`

## Commands
- /land claim
- /land unclaim
- /land trust <player>
- /land untrust <player>
- /land info
- /playtime
- /playtime top

## Permissions (LuckPerms)
### Build / Land
- bonescraft.land.claim       -> who can buy/claim land (your “builder rank”)
- bonescraft.land.globalbuild -> who can build anywhere (no claim needed)
- bonescraft.land.admin       -> bypass everything (Owner/Co-Owner/Admin)
### Chest looting
- bonescraft.land.loot        -> can take/move items from other players' containers
### Playtime
- bonescraft.playtime.view    -> view your own playtime
- bonescraft.playtime.admin   -> view top list

## No-PC build (recommended)
If you don't have a PC/build tools, build the .jar via **GitHub Actions**:

1) Create a new GitHub repository and upload this folder contents.
2) In GitHub go to **Actions** → run **Build Plugin** (or just push once).
3) After it finishes: Actions → latest run → **Artifacts** → download `BonescraftLand-jar`.
4) Upload the `.jar` to your rented server: `plugins/` folder.
5) Restart server.

## Config
After first start:
`plugins/BonescraftLand/config.yml`
