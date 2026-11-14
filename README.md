# vkauth

**English** | [فارسی](README.FA.md)

A lightweight register / login plugin for **Paper 1.21+** servers.
Players must register once and log in on every join. Until they do, they are frozen and cannot chat, run commands, break/place blocks, or interact with anything.

> Created by **BenyVK**
> Telegram: <https://t.me/i36VK> · GitHub: <https://github.com/BenyVK>

---

## Features

- `/register`, `/login` and `/logout` with short aliases (`/reg`, `/log`)
- Passwords hashed with **PBKDF2-HMAC-SHA256** (120,000 iterations, random salt per password) — never stored in plain text
- **Case-sensitive or case-insensitive accounts** (`replay-id-account`) — decide whether `BenyVK` and `BEnyVK` are two accounts or one
- **Wrong-name protection** (`incorrect-installation`) — a player who joins with the wrong upper/lower case (e.g. `BEnyVK` instead of `BenyVK`) is kicked and told which name to use
- Storage in **YAML**, **MySQL**, or **both at once**, with automatic migration between them
- **Link with RankVK** (`online-rankvk`): when an account is deleted, that player's RankVK data (permissions, prefix, suffix) is deleted too
- **Forget deleted players** (`delete-world-data`, optional): the player's world files are deleted together with the account
- **One shared table with RankVK** (`match-signalVK`, optional): accounts and ranks of a player are saved together in one database table
- Unauthenticated players are completely locked: movement, chat, commands, interaction, inventory, item pick-up/drop, buckets, damage (given and taken) are all blocked
- Blocked commands are also removed from the client's tab-completion
- Optional waiting room (`auth-map`) and optional fixed spawn after login (`start_auth_all`)
- Optional floating **3D text** shown only to the player who is logging in
- Optional **TAB list names** such as `LOGIN_USER_3` / `REGISTER_USER_7` (daily counters)
- Hide other players from not-yet-logged-in players
- Login timeout, wrong-password limit and reminder messages
- "Last login" date shown after a successful login
- Every message is editable in `configvk.yml`

---

## Requirements

- **Paper** 1.21 or newer (the plugin uses Paper's chat event and Adventure API — Spigot/Bukkit alone is not supported)
- **Java 21**
- For MySQL storage: a MySQL/MariaDB server and the MySQL JDBC driver (`com.mysql.cj.jdbc.Driver`) available on the server. The plugin does not bundle a driver itself.

---

## Installation

1. Put `vkauth.jar` into your server's `plugins/` folder.
2. Start the server once. `plugins/vkauth/configvk.yml` is created.
3. Edit `configvk.yml` to your liking.
4. **Restart the server** (the config is only read at startup).

---

## Commands

| Command | Aliases | Description |
|---|---|---|
| `/register <password> <confirm>` | `/reg` | Create your account |
| `/login <password>` | `/log` | Log in to your account |
| `/logout` | — | Leave the server (kicks you with a goodbye message) |
| `/vkauth delete <name>` | `/authvk` | **Admin:** delete the account of a player (permission `vkauth.admin`) |
| `/vkauth purge [confirm]` | `/authvk` | **Admin:** list (or, with `confirm`, forget) offline players who have no account (permission `vkauth.admin`) |

These commands are the only ones usable before logging in. You can change that list with `auth-commands`. The `vkauth:` prefix is accepted too (for example `/vkauth:login`).

`/register`, `/login` and `/logout` can be used by everyone. The only admin command is `/vkauth delete <name>`:

| Permission | Default | Description |
|---|---|---|
| `vkauth.admin` | op | `/vkauth delete <name>` and `/vkauth purge` |

`/vkauth delete <name>` removes the account from every enabled storage and kicks the player if he is online (`messages.account-deleted-kick`). With `online-rankvk: true` the player's RankVK data is deleted right away too. With `replay-id-account: false` every spelling of the name is deleted. With `delete-world-data: true` the world files of the player are deleted too.

---

## Configuration (`configvk.yml`)

Colors use `&` codes (`&a`, `&c`, …). Placeholders use `{braces}`.

### Accounts: `replay-id-account`

```yml
replay-id-account: true
```

| Value | Behavior |
|---|---|
| `true` | Upper/lower case of the name matters. `BenyVK` and `BEnyVK` are **two different accounts**, each with its own password. |
| `false` | Upper/lower case is ignored. `BenyVK`, `BEnyVK` and every other spelling are **one account with one password**. |

**Switching from `true` to `false`:** old accounts are found by name (ignoring case) the first time that player joins, and copied to the shared account automatically. If several spellings were registered before, the **oldest registration wins**.
**Switching from `false` to `true`:** this is not automatic — players will have to register again under their exact name.

> This option matters on offline-mode servers, where the player's UUID is derived from the exact spelling of the name. On online-mode servers Mojang does not allow names that differ only by case, so there is no visible difference.

### Wrong name check: `incorrect-installation`

```yml
incorrect-installation: false
```

| Value | Behavior |
|---|---|
| `true` | A player whose name differs from the registered name **only by upper/lower case** is **kicked**, and the kick message tells them which name to use. |
| `false` | No check (default). |

Example with `incorrect-installation: true`: the account was registered as `BenyVK`. A player joins as `BEnyVK` and is kicked with:

```
Your name must be BenyVK to log in.
You joined as BEnyVK. Please change your name and join again.
```

The kick text is `messages.wrong-name-kick` and is fully editable (see [Messages](#messages)).

It works with both values of `replay-id-account`:

- with `replay-id-account: false` the shared account is found and its registered spelling is compared with the name the player joined with;
- with `replay-id-account: true` a player who joins with a different spelling would normally just get a brand-new account — with this option on, they are kicked instead, so look-alike accounts (`Admin` / `ADmin`) cannot be created.

The check only looks at accounts that already exist. Brand-new names register normally.

### Storage

```yml
storage:
  type: "yaml"          # yaml | mysql | mysql,yaml
  migrate: true
  delete-old-after-migrate: true
  mysql:
    host: "127.0.0.1"
    port: 3306
    database: "vkauth"
    username: "root"
    password: ""
    table: "vkauth_accounts"
```

| Option | Description |
|---|---|
| `type: yaml` | Accounts are saved in `plugins/vkauth/accounts.yml` |
| `type: mysql` | Accounts are saved in the MySQL table |
| `type: mysql,yaml` | Saved in **both**; read from the first one, with the second as fallback |
| `migrate` | When you change the storage type, accounts from the storage that is no longer enabled are copied into the enabled one(s). With `mysql,yaml` both are kept in sync at startup. |
| `delete-old-after-migrate` | After a fully successful transfer, the old storage (the `accounts.yml` file or the MySQL table) is deleted |

The MySQL table and its columns (`registered`, `last_login`) are created and repaired automatically. To keep accounts and RankVK data in one table, see [`match-signalVK`](#one-shared-table-with-rankvk-match-signalvk).

### Link with RankVK: `online-rankvk`

```yml
online-rankvk: true
link-check-minutes: 5
```

| Value | Behavior |
|---|---|
| `true` | When an account is deleted in vkauth, the RankVK data of that player (permissions, prefix, suffix) is deleted as well, so nothing is left behind. |
| `false` | vkauth never touches RankVK. |

- RankVK has the matching switch `online-authvk` in its `configvk.yml`. **Both must be `true`**, otherwise nothing is deleted in RankVK.
- Both plugins must run on the same server. It works with any storage of RankVK (json, yaml, sqlite, mysql, ...), and with yaml or mysql in vkauth.
- It works for `/vkauth delete <name>` and also for accounts deleted **by hand** (in `accounts.yml` or in the database). vkauth remembers who had an account (`linked-accounts.yml`) and every `link-check-minutes` minutes (and at startup) removes the RankVK data of everyone who vanished. `accounts.yml` is only re-read at startup, so edit it while the server is off.
- The first run only takes a snapshot and deletes nothing. If the storage cannot be read, or suddenly looks completely empty, nothing is deleted.
- Accounts that were deleted **before** you turned this on are not cleaned up automatically.

### Forget deleted players: `delete-world-data`

```yml
delete-world-data: false
```

| Value | Behavior |
|---|---|
| `true` | When an account is deleted, the player's files in **every world** (`playerdata/<uuid>.dat` and `.dat_old`, `stats/<uuid>.json`, `advancements/<uuid>.json`) are deleted too. The server (and the RankVK web editor list of offline players) then forgets him. An online player is kicked first and his files are deleted about 2 seconds later. |
| `false` | World files are never touched (default). |

- It reacts to accounts that are **deleted** (`/vkauth delete`, or by hand in `accounts.yml` / the database), with the same detection as `online-rankvk` (checks every `link-check-minutes` minutes and at startup). It works even when `online-rankvk` is `false`.
- Only the player's own files are removed. Builds, and data of other plugins (economy, Essentials, ...), stay.
- `usercache.json` is not touched.
- vkauth never deletes world data of players just because they have no account: a player who joined but has not registered yet would lose his data, and a database outage would look like "everybody was deleted". For players that have **no account right now** use `/vkauth purge`:
  - `/vkauth purge` only **lists** offline players who have no account (players who joined but did not register are included!)
  - `/vkauth purge confirm` forgets them: their RankVK data (with `online-rankvk: true`) and their world files (with `delete-world-data: true`)
  - nothing is done when the accounts cannot be read or there are none
- Make a backup of your worlds before the first `purge confirm`.

### One shared table with RankVK: `match-signalVK`

```yml
match-signalVK: false
match-table: players_shared
```

| Value | Behavior |
|---|---|
| `false` | vkauth keeps its own table (`storage.mysql.table`) (default). |
| `true` | The account and the RankVK data of a player live in **one row of one table** (`match-table`) of the same MySQL database, for example database `minecraft`, table `players`. |

It only turns on when **all** of these are true, otherwise the console says why and the normal storage is used:

- `match-signalVK: true` in the config of vkauth **and** of RankVK
- vkauth uses MySQL (`storage.type` contains `mysql`) and RankVK uses `storage-method: mysql` or `mariadb`
- both point at the same database (same host, port and database) and have the same `match-table`
- `match-table` is **not** the same as `storage.mysql.table`

Things to know:

- Each row holds the login data (`auth_*` columns) and the rank data (`rank_*` columns). Each plugin only writes its own columns.
- Accounts of the old table (and of `accounts.yml`) are copied into the shared table once. The old table is deleted or renamed to `..._migrated`, depending on `delete-old-after-migrate`.
- With `match-signalVK: true` every player has one row under his own UUID. `replay-id-account: false` still works (the oldest registration wins).
- Turning `match-signalVK` off later does not copy the accounts back; they stay in the shared table.
- If the table `match-table` already exists and has no `uuid` column, it is not touched and the shared mode stays off.

### Waiting room and spawn

```yml
auth-map: false
pos-auth-world: "world"
pos-auth-start: "0.5,100,0.5,0"

start_auth_all: false
start_auth_all_replay: true
start_auth_world: "world"
start_auth_pos: "0.5,100,0.5,0"
```

Positions use the format `X,Y,Z,A` (`A` = yaw / facing angle).

- `auth-map: true` — players who are not logged in are teleported to `pos-auth-start`, and go back to where they were after logging in.
- `start_auth_all: true` — after login/register the player is teleported to `start_auth_pos`.
- `start_auth_all_replay` (only when `start_auth_all` is `true`):
  - `true` — **every** login spawns the player at `start_auth_pos`
  - `false` — only the **first** time (right after `/register`); later logins return the player to where they were

### Hiding players

```yml
auth_hide_players: false
```

`true` = a player who is not logged in cannot see other players (works with `auth-map` or on its own).

### Allowed commands before login

```yml
auth-commands:
  - login
  - log
  - register
  - reg
  - logout
```

Every other command is blocked on the server and removed from the tab-completion suggestions sent to the player.

### 3D text

```yml
text3d: true
text3d_note: "&b&lWelcome &f{player}\n&7Please log in or register"
text3d_scale: 1.5
text3d_distance: 2.5
text3d_height: 0.3
```

A floating text in front of the player while logging in. **Only that player can see it**, and it disappears after login. `\n` = new line, `{player}` = the player's name.

### TAB list names

```yml
tab_names: true
tab_login_format: "LOGIN_USER_{number}"
tab_register_format: "REGISTER_USER_{number}"
```

While a player is logging in / registering, their TAB name is replaced so their real name is not shown. `{number}` is how many logins / registrations started **today** (resets every day, saved in `counters.yml`).

### Last login and time zone

```yml
last-login-format: "yyyy-MM-dd HH:mm:ss"
time-zone: ""
```

`last-login-format` follows Java's [`DateTimeFormatter`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/format/DateTimeFormatter.html) patterns. `time-zone` takes values like `Europe/Berlin` or `Asia/Tehran`; empty means the server's time zone.

### Security

```yml
security:
  min-password-length: 6
  max-password-length: 32
  max-login-attempts: 5          # 0 = unlimited
  login-timeout-seconds: 60      # 0 = no limit
  reminder-interval-seconds: 10  # 0 = off
```

### Messages

Every message shown to players lives under `messages:` and can be changed freely. Available placeholders:

| Placeholder | Where it works |
|---|---|
| `{prefix}` | Any message (value of `messages.prefix`) |
| `{player}` | `must-register`, `must-login`, `wrong-name-kick`, `text3d_note` |
| `{name}` | `wrong-name-kick` (the name the account was registered with) |
| `{min}` / `{max}` | `password-too-short` / `password-too-long` |
| `{date}` | `last-login` |
| `{attempts}`, `{max}`, `{left}` | `wrong-password` |
| `{seconds}` | `timeout-kick` |
| `{player}`, `{count}` | `admin-deleted` (`{player}` also in `admin-not-found`) |
| `{count}`, `{players}` | `purge-list` (`{count}` also in `purge-done`) |

The admin commands use the messages `no-permission`, `admin-usage`, `admin-not-found`, `admin-deleted`, `account-deleted-kick`, `purge-refused`, `purge-none`, `purge-list` and `purge-done`.

---

## How it works

1. A player joins and is immediately locked. They are optionally moved to the waiting room, hidden from others and given a temporary TAB name.
2. The account is loaded **asynchronously** from storage, so the server thread is never blocked by MySQL or disk access.
3. The player is asked to `/register` (new) or `/login` (existing).
4. Password hashing and verification also run asynchronously.
5. On success the lock is removed, the player is teleported (if configured) and their last-login time is saved.
6. Too many wrong passwords, or taking too long, kicks the player.

If the storage cannot be read, the player is kicked with `messages.storage-error` instead of being let in unprotected.

---

## Files created by the plugin

| File | Purpose |
|---|---|
| `plugins/vkauth/configvk.yml` | Settings and messages |
| `plugins/vkauth/accounts.yml` | Accounts (when YAML storage is enabled) |
| `plugins/vkauth/counters.yml` | Daily login/register counters for TAB names |
| `plugins/vkauth/linked-accounts.yml` | Who had an account at the last check (used by `online-rankvk`). Do not edit |

---

## Building

The source is a standard Paper plugin (`ir.gharri.vkauth`, main class `ir.gharri.vkauth.Vkauth`, `api-version: 1.21`). Build it with Java 21 against the Paper 1.21 API using your Maven or Gradle setup, then drop the resulting jar into `plugins/`.

---

## Support

Questions or ideas? Open an issue on GitHub or contact **BenyVK** on Telegram: <https://t.me/i36VK>
