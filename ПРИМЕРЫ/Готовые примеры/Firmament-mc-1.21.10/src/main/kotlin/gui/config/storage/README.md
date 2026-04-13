<!--
SPDX-FileCopyrightText: 2025 Linnea Gräf <nea@nea.moe>

SPDX-License-Identifier: CC0-1.0
-->

# Plan for the 2026 Config Renewal of Firmament

The current config system in Firmament is not growing at a reasonable pace. Here is a list of my grievances with it:

- the config files are split, resulting in making migrations between different config files (which might load in
  different order) difficult
- it is difficult to detect extraneous properties / files, because not all files are loaded and consumed at once
- profile specific data should be in a different hierarchy. the current hierarchy of `profiles/topic/<uuid>.json` orders
  data from different profiles to be closer than data from the same profile. this also contributes to the two former
  problems.

## Goals

- i want to retain having multiple different files for different topics, as well as a folder structure that makes sense
  for profiles.
- i want to split up "storage" type data, with "config" type data
- i want to support partial loads with some broken files (resetting the files that are broken)
- i want to support backups on any detected error (or simply at will)
	- notably i do not care about the structure of the backups much. even just a all json files merged backup is fine
	  for me, for now.

## Implementation

### FirstLevelSplitJsonFolder

One of the basic components of this new config folder is a `FirstLevelSplitJsonFolder`. A `FLSJF` takes in a folder
containing multiple JSON-files and loads all of them unconditionally. Each file is then inserted side by side into a
json object, to be processed further by other mechanisms.

In essence the `FLSJF` takes a folder structure like this:

```
file-1.json
file-2.json
file-3.json
```

and turns it into a single merged json object:

```json
{
	"file-1": "the json content of file-1.json",
	"file-2": "the json content of file-2.json",
	"file-3": "the json content of file-3.json"
}
```

As with any stage of the implementation, any unparsable files shall be copied over to a backup spot and discarded.

Nota bene: Folders are wholesale ignored.

### Config folders

Firmament stores all configs and data in the root config folder `./config/firmament`.

- Any config data is stored as an [`FLSJF`](#firstlevelsplitjsonfolder) in the root config folder
- Any generic storage data is stored as an [`FLSJF`](#firstlevelsplitjsonfolder) in `${rootConfigFolder}/storage/`.
- Any profile specific storage data is stored as an [`FLSJF`](#firstlevelsplitjsonfolder) for each profile in `${rootConfigFolder}/profileStorage/${profileUuid}/`.
- Any backup data is stored in `${rootConfigFolder}/backups/${launchId}/${loadId}/${fileName}`.
  - Where `launchId` is `${currentLaunchTimestamp}-${random()}` to avoid collisions.
  - Where `loadId` depends on which stage of the config load we are doing (`merge`/`upgrade`/etc.) and what type of config we are loading (`profileSpecific`/`config`/etc.).
  - And where `fileName` may be a relative filename of where this data was originally found or some internal descriptor for the merged data stage we are on.
