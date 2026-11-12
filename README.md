# Cable Cloak

Fabric mod for Minecraft 26.3 that hides cables behind facades copying the look of any block.
Currently supports the cables of [OmniVault](https://modrinth.com/mod/omnivault) (Nexus Cable and Inventory Cable, any colour).

## Use

1. Craft **Cloak Frames** (8 from 4 sticks + 4 iron nuggets, sticks in the corners).
2. Right-click a cable with a frame (or the face of a block next to it, aiming into the cable's spot like
   placing a block there): the cable gets an empty frame and becomes a full block.
3. Right-click the frame (again, directly or from a neighbouring face) with any block item, vanilla or modded,
   to give it that block's look.
   One block is used up; right-click with another block to swap it (the old one comes back).
4. Sneak + right-click with an empty hand takes the look off, then the frame.
   Breaking the cable drops the frame and the block as well.

Sneak + right-click with a block places it against the facade as usual.
Blocks drawn only by a block entity renderer (chests, signs, beds…) and invisible blocks can't be used.

## How it works

- A mixin makes OmniVault's `CableBlock` an `EntityBlock`; `CloakBlockEntity` holds the frame, the copied
  `BlockState` and the item paid for it, and syncs to clients. The cable block itself is untouched, so
  networks, dyeing and connections behave exactly as before.
- `CableBlock#getShape` is extended with the facade's shape, so cloaked cables are solid.
- On the client every cable model is wrapped (`CloakedCableModel`): a cloaked cable emits the copied block's
  model (tints baked in, smooth lighting forced back on since cables emit light), plus the cable itself when the copy isn't a full opaque block (glass, stairs, leaves…).
- `CloakSupport` decides what counts as a cable — the one place to extend for other cable mods.

## Development

Toolchain comes from the nix flake (`nix develop`, or direnv).

```sh
nix develop -c ./gradlew build              # build + server gametests
nix develop -c ./gradlew runGameTest        # server gametests only
nix develop -c ./gradlew runClientGameTest  # renders every cloak state, screenshots in build/run/clientGameTest/gallery
nix develop -c ./gradlew runClient
# Same client test with Sodium + Iris + MoreCulling and a shader pack, on an isolated Xvfb display (software GL).
# Fails unless Iris reports the pack active; screenshots in build/run/clientGameTest/gallery_shaders.
# A <pack>.zip.txt next to the pack is copied as its settings (Photon needs SH_SKYLIGHT=false on llvmpipe).
nix develop -c python3 scripts/headless.py ./gradlew runClientGameTest -PshaderPack=/path/to/photon_v1.4.zip
nix develop -c python3 scripts/gen_art.py src/main/resources/assets/cable_cloak/textures/block/cloak_frame.png \
    src/main/resources/assets/cable_cloak/icon.png docs/icon.png
```

Publishing: `./gradlew modrinth` (add `-PmodrinthDryRun` to preview), `./gradlew modrinthSyncBody` for the page.
