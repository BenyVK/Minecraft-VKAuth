<div dir="rtl">

# vkauth

[English](README.md) | **فارسی**

یک پلاگین سبک ثبت‌نام و ورود برای سرورهای **Paper 1.21+**.
بازیکن‌ها یک بار ثبت‌نام می‌کنن و هر بار که وارد سرور میشن باید لاگین کنن. تا وقتی لاگین نکردن، بازیکن قفل میشه و نمیتونه چت کنه، دستور بزنه، بلاک بشکنه یا بذاره، یا با چیزی تعامل داشته باشه.

> ساخته‌شده توسط **BenyVK**
> تلگرام: <https://t.me/i36VK> · گیت‌هاب: <https://github.com/BenyVK>

---

## امکانات

- دستورهای `/register`، `/login` و `/logout` با میانبرهای `/reg` و `/log`
- رمزها با **PBKDF2-HMAC-SHA256** هش میشن (۱۲۰,۰۰۰ تکرار و salt تصادفی برای هر رمز) و هیچ‌وقت به‌صورت متن ساده ذخیره نمیشن
- **اکانت حساس یا غیرحساس به حروف بزرگ و کوچک** (`replay-id-account`): مشخص کن `BenyVK` و `BEnyVK` دو اکانت جدا باشن یا یکی
- **محافظت از اسم اشتباه** (`incorrect-installation`): بازیکنی که با حروف بزرگ/کوچک اشتباه وارد میشه (مثلاً `BEnyVK` به‌جای `BenyVK`) کیک میشه و بهش گفته میشه با چه اسمی بیاد
- ذخیره‌سازی در **YAML**، **MySQL** یا **هر دو همزمان**، با انتقال خودکار بین‌شون
- **اتصال به RankVK** (`online-rankvk`): وقتی اکانتی حذف بشه، اطلاعات RankVK همون بازیکن (پرمیشن‌ها، پیشوند و پسوند) هم حذف میشه
- **فراموش کردن بازیکن حذف‌شده** (`delete-world-data`، اختیاری): فایل‌های بازیکن توی worldها هم همراه اکانت حذف میشن
- **یک جدول مشترک با RankVK** (`match-signalVK`، اختیاری): اکانت و رنک هر بازیکن با هم توی یه جدول دیتابیس ذخیره میشه
- بازیکنِ لاگین‌نکرده کاملاً قفله: حرکت، چت، دستورها، تعامل، اینونتوری، برداشتن و انداختن آیتم، سطل، و آسیب زدن/دیدن همه بلاک میشن
- دستورهای بلاک‌شده از tab-completion کلاینت هم حذف میشن
- اتاق انتظار اختیاری (`auth-map`) و اسپاون ثابت اختیاری بعد از لاگین (`start_auth_all`)
- **متن سه‌بعدی** شناور اختیاری که فقط برای همون بازیکنِ در حال لاگین دیده میشه
- **اسم TAB** اختیاری مثل `LOGIN_USER_3` یا `REGISTER_USER_7` (شمارنده‌ی روزانه)
- مخفی کردن بقیه‌ی بازیکن‌ها از دید بازیکنِ لاگین‌نکرده
- محدودیت زمانی لاگین، محدودیت تعداد رمز اشتباه و پیام‌های یادآوری
- نمایش تاریخ «آخرین ورود» بعد از لاگین موفق
- همه‌ی پیام‌ها توی `configvk.yml` قابل ویرایشن

---

## نیازمندی‌ها

- **Paper** نسخه‌ی 1.21 یا بالاتر (پلاگین از event چت Paper و Adventure API استفاده میکنه؛ Spigot/Bukkit تنها پشتیبانی نمیشه)
- **Java 21**
- برای ذخیره‌سازی MySQL: یک سرور MySQL/MariaDB و درایور JDBC مای‌اس‌کیوال (`com.mysql.cj.jdbc.Driver`) روی سرور. پلاگین خودش درایور همراه نداره.

---

## نصب

1. فایل `vkauth.jar` رو توی پوشه‌ی `plugins/` سرور بذار.
2. سرور رو یک بار روشن کن تا `plugins/vkauth/configvk.yml` ساخته بشه.
3. `configvk.yml` رو به دلخواه ویرایش کن.
4. **سرور رو ریستارت کن** (کانفیگ فقط موقع استارت خونده میشه).

---

## دستورها

| دستور | میانبر | توضیح |
|---|---|---|
| `/register <password> <confirm>` | `/reg` | ساخت اکانت |
| `/login <password>` | `/log` | ورود به اکانت |
| `/logout` | — | خروج از سرور (بازیکن با پیام خداحافظی کیک میشه) |
| `/vkauth delete <name>` | `/authvk` | **ادمین:** حذف اکانت یه بازیکن (پرمیشن `vkauth.admin`) |
| `/vkauth purge [confirm]` | `/authvk` | **ادمین:** لیست (یا با `confirm` فراموش کردنِ) بازیکن‌های آفلاینی که اکانت ندارن (پرمیشن `vkauth.admin`) |

این دستورها تنها دستورهایی هستن که قبل از لاگین قابل استفاده‌ان. لیستشون رو با `auth-commands` میتونی عوض کنی. پیشوند `vkauth:` هم پذیرفته میشه (مثلاً `/vkauth:login`).

`/register`، `/login` و `/logout` رو همه میتونن بزنن. تنها دستور ادمین `/vkauth delete <name>` هست:

| پرمیشن | پیش‌فرض | توضیح |
|---|---|---|
| `vkauth.admin` | op | `/vkauth delete <name>` و `/vkauth purge` |

`/vkauth delete <name>` اکانت رو از همه‌ی ذخیره‌سازی‌های فعال حذف میکنه و اگه بازیکن آنلاین باشه کیکش میکنه (`messages.account-deleted-kick`). با `online-rankvk: true` اطلاعات RankVK اون بازیکن هم همون لحظه حذف میشه. با `replay-id-account: false` همه‌ی املاهای اون اسم حذف میشن. با `delete-world-data: true` فایل‌های بازیکن توی worldها هم حذف میشن.

---

## تنظیمات (`configvk.yml`)

رنگ‌ها با کدهای `&` هستن (`&a`، `&c`، …). placeholderها داخل `{}` میان.

### اکانت‌ها: `replay-id-account`

```yml
replay-id-account: true
```

| مقدار | رفتار |
|---|---|
| `true` | حروف بزرگ و کوچک اسم مهمه. `BenyVK` و `BEnyVK` **دو اکانت جدا** هستن و هرکدوم رمز خودش رو داره. |
| `false` | حروف بزرگ و کوچک نادیده گرفته میشه. `BenyVK`، `BEnyVK` و هر املای دیگه **یک اکانت با یک رمز** هستن. |

**تغییر از `true` به `false`:** اکانت‌های قدیمی بار اول که اون بازیکن وارد میشه با اسمشون (بدون حساسیت به حروف) پیدا و خودکار به اکانت مشترک کپی میشن. اگه قبلاً چند املا جدا ثبت‌نام کرده بودن، **اکانتی که زودتر ثبت شده** برنده‌ست.
**تغییر از `false` به `true`:** خودکار نیست و بازیکن‌ها باید با اسم دقیقشون دوباره ثبت‌نام کنن.

> این گزینه توی سرورهای آفلاین‌مود مهمه، چون UUID بازیکن از املای دقیق اسم ساخته میشه. توی سرورهای آنلاین‌مود، Mojang اسم‌هایی که فقط توی حروف بزرگ/کوچک فرق دارن رو قبول نمیکنه، پس تفاوتی دیده نمیشه.

### بررسی اسم اشتباه: `incorrect-installation`

```yml
incorrect-installation: false
```

| مقدار | رفتار |
|---|---|
| `true` | بازیکنی که اسمش **فقط توی حروف بزرگ و کوچک** با اسم ثبت‌شده فرق داره **کیک** میشه و توی پیام کیک بهش گفته میشه با چه اسمی بیاد. |
| `false` | هیچ بررسی‌ای انجام نمیشه (پیش‌فرض). |

مثال با `incorrect-installation: true`: اکانت با اسم `BenyVK` ثبت شده. بازیکنی با اسم `BEnyVK` وارد میشه و با این پیام کیک میشه:

```
Your name must be BenyVK to log in.
You joined as BEnyVK. Please change your name and join again.
```

متن کیک همون `messages.wrong-name-kick` هست و کاملاً قابل تغییره (بخش [پیام‌ها](#پیامها) رو ببین).

با هر دو مقدار `replay-id-account` کار میکنه:

- با `replay-id-account: false` اکانت مشترک پیدا میشه و املای ثبت‌شده‌ش با اسمی که بازیکن با اون وارد شده مقایسه میشه؛
- با `replay-id-account: true` بازیکنی که با املای متفاوت وارد بشه معمولاً یه اکانت کاملاً جدید میگیره. با روشن بودن این گزینه به‌جاش کیک میشه، پس اکانت‌های شبیه‌هم (`Admin` / `ADmin`) ساخته نمیشن.

این بررسی فقط اکانت‌هایی که از قبل وجود دارن رو چک میکنه. اسم‌های کاملاً جدید عادی ثبت‌نام میکنن.

### ذخیره‌سازی

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

| گزینه | توضیح |
|---|---|
| `type: yaml` | اکانت‌ها توی `plugins/vkauth/accounts.yml` ذخیره میشن |
| `type: mysql` | اکانت‌ها توی جدول MySQL ذخیره میشن |
| `type: mysql,yaml` | توی **هر دو** ذخیره میشه؛ از اولی خونده میشه و دومی پشتیبانه |
| `migrate` | وقتی نوع ذخیره‌سازی رو عوض میکنی، اکانت‌های ذخیره‌سازیِ غیرفعال به ذخیره‌سازی(های) فعال کپی میشن. با `mysql,yaml` هر دو موقع استارت همگام میشن. |
| `delete-old-after-migrate` | بعد از انتقال کاملاً موفق، ذخیره‌سازی قدیمی (فایل `accounts.yml` یا جدول MySQL) پاک میشه |

جدول MySQL و ستون‌هاش (`registered`، `last_login`) خودکار ساخته و اصلاح میشن. برای نگه داشتن اکانت و اطلاعات RankVK توی یک جدول، بخش [`match-signalVK`](#یک-جدول-مشترک-با-rankvk-match-signalvk) رو ببین.

### اتصال به RankVK: `online-rankvk`

```yml
online-rankvk: true
link-check-minutes: 5
```

| مقدار | رفتار |
|---|---|
| `true` | وقتی اکانتی توی vkauth حذف بشه، اطلاعات RankVK همون بازیکن (پرمیشن‌ها، پیشوند و پسوند) هم حذف میشه و چیزی جا نمیمونه. |
| `false` | vkauth هیچ‌وقت به RankVK کاری نداره. |

- RankVK سوییچ مشابهی به اسم `online-authvk` توی `configvk.yml` خودش داره. **هر دو باید `true` باشن**، وگرنه توی RankVK چیزی حذف نمیشه.
- هر دو پلاگین باید روی یک سرور باشن. با هر نوع ذخیره‌سازیِ RankVK (json، yaml، sqlite، mysql و ...) و با yaml یا mysql توی vkauth کار میکنه.
- هم برای `/vkauth delete <name>` کار میکنه و هم برای اکانت‌هایی که **دستی** حذف میشن (از `accounts.yml` یا دیتابیس). vkauth یادش میمونه چه کسایی اکانت داشتن (`linked-accounts.yml`) و هر `link-check-minutes` دقیقه (و موقع استارت) اطلاعات RankVK هر کسی که ناپدید شده رو پاک میکنه. `accounts.yml` فقط موقع استارت دوباره خونده میشه، پس وقتی سرور خاموشه ویرایشش کن.
- اجرای اول فقط یه عکس از وضعیت میگیره و چیزی پاک نمیکنه. اگه ذخیره‌سازی خونده نشه یا یهو کاملاً خالی بشه، چیزی پاک نمیشه.
- اکانت‌هایی که **قبل از** روشن کردن این گزینه حذف شدن خودکار پاک نمیشن.

### فراموش کردن بازیکن حذف‌شده: `delete-world-data`

```yml
delete-world-data: false
```

| مقدار | رفتار |
|---|---|
| `true` | وقتی اکانتی حذف بشه، فایل‌های اون بازیکن توی **همه‌ی worldها** (`playerdata/<uuid>.dat` و `.dat_old`، `stats/<uuid>.json`، `advancements/<uuid>.json`) هم حذف میشن. بعدش سرور (و لیست بازیکن‌های آفلاینِ ادیتور وب RankVK) دیگه اون بازیکن رو یادش نیست. بازیکنِ آنلاین اول کیک میشه و فایل‌هاش حدود ۲ ثانیه بعد حذف میشن. |
| `false` | به فایل‌های world دست زده نمیشه (پیش‌فرض). |

- فقط به اکانت‌هایی که **حذف** میشن واکنش نشون میده (`/vkauth delete` یا حذف دستی از `accounts.yml` / دیتابیس)، با همون تشخیصِ `online-rankvk` (هر `link-check-minutes` دقیقه و موقع استارت). حتی وقتی `online-rankvk` برابر `false` باشه هم کار میکنه.
- فقط فایل‌های خود بازیکن پاک میشن. ساخته‌ها و دیتای پلاگین‌های دیگه (اکونومی، Essentials و ...) میمونن.
- به `usercache.json` دست زده نمیشه.
- vkauth فقط به‌خاطر نداشتن اکانت، دیتای بازیکن رو خودکار پاک نمیکنه: بازیکنی که وارد شده ولی هنوز ثبت‌نام نکرده دیتاش میرفت و قطعی دیتابیس هم شبیه «همه حذف شدن» دیده میشد. برای بازیکن‌هایی که **همین الان اکانت ندارن** از `/vkauth purge` استفاده کن:
  - `/vkauth purge` فقط بازیکن‌های آفلاینی که اکانت ندارن رو **لیست** میکنه (بازیکن‌هایی که وارد شدن ولی ثبت‌نام نکردن هم توشن!)
  - `/vkauth purge confirm` فراموششون میکنه: دیتای RankVK (با `online-rankvk: true`) و فایل‌های worldشون (با `delete-world-data: true`)
  - اگه اکانت‌ها خونده نشن یا هیچ اکانتی نباشه، کاری انجام نمیشه
- قبل از اولین `purge confirm` از worldها بکاپ بگیر.

### یک جدول مشترک با RankVK: `match-signalVK`

```yml
match-signalVK: false
match-table: players_shared
```

| مقدار | رفتار |
|---|---|
| `false` | vkauth جدول خودش رو نگه میداره (`storage.mysql.table`) (پیش‌فرض). |
| `true` | اکانت و اطلاعات RankVK هر بازیکن توی **یک ردیف از یک جدول** (`match-table`) از همون دیتابیس MySQL ذخیره میشه؛ مثلاً دیتابیس `minecraft` و جدول `players`. |

فقط وقتی روشن میشه که **همه‌ی** این شرط‌ها برقرار باشن؛ وگرنه کنسول دلیلش رو میگه و از ذخیره‌سازی عادی استفاده میشه:

- `match-signalVK: true` هم توی کانفیگ vkauth و **هم** RankVK
- vkauth از MySQL استفاده کنه (`storage.type` شامل `mysql` باشه) و RankVK هم `storage-method: mysql` یا `mariadb` باشه
- هر دو به یک دیتابیس اشاره کنن (همون host، port و database) و `match-table` یکسان داشته باشن
- `match-table` با `storage.mysql.table` **یکی نباشه**

نکته‌ها:

- هر ردیف هم اطلاعات ورود (ستون‌های `auth_*`) و هم اطلاعات رنک (ستون‌های `rank_*`) رو داره. هر پلاگین فقط ستون‌های خودش رو مینویسه.
- اکانت‌های جدول قدیمی (و `accounts.yml`) یک بار به جدول مشترک کپی میشن. جدول قدیمی بسته به `delete-old-after-migrate` پاک میشه یا اسمش به `..._migrated` تغییر میکنه.
- با `match-signalVK: true` هر بازیکن یک ردیف با UUID خودش داره. `replay-id-account: false` هم کار میکنه (اکانتِ زودتر ثبت‌شده برنده‌ست).
- اگه بعداً `match-signalVK` رو خاموش کنی، اکانت‌ها به جدول قبلی برنمیگردن و توی جدول مشترک میمونن.
- اگه جدولِ `match-table` از قبل وجود داشته باشه و ستون `uuid` نداشته باشه، بهش دست زده نمیشه و حالت مشترک خاموش میمونه.

### اتاق انتظار و اسپاون

```yml
auth-map: false
pos-auth-world: "world"
pos-auth-start: "0.5,100,0.5,0"

start_auth_all: false
start_auth_all_replay: true
start_auth_world: "world"
start_auth_pos: "0.5,100,0.5,0"
```

مختصات با فرمت `X,Y,Z,A` نوشته میشن (`A` = زاویه‌ی نگاه / yaw).

- `auth-map: true`: بازیکن‌های لاگین‌نکرده به `pos-auth-start` تلپورت میشن و بعد از لاگین به جای قبلیشون برمیگردن.
- `start_auth_all: true`: بعد از لاگین/ثبت‌نام، بازیکن به `start_auth_pos` تلپورت میشه.
- `start_auth_all_replay` (فقط وقتی `start_auth_all` برابر `true` باشه):
  - `true`: **هر بار** که لاگین میکنه توی `start_auth_pos` اسپاون میشه
  - `false`: فقط **بار اول** (درست بعد از `/register`)؛ لاگین‌های بعدی بازیکن رو به جای قبلیش برمیگردونن

### مخفی کردن بازیکن‌ها

```yml
auth_hide_players: false
```

`true` یعنی بازیکنِ لاگین‌نکرده بقیه‌ی بازیکن‌ها رو نمیبینه (با `auth-map` یا به‌تنهایی کار میکنه).

### دستورهای مجاز قبل از لاگین

```yml
auth-commands:
  - login
  - log
  - register
  - reg
  - logout
```

بقیه‌ی دستورها سمت سرور بلاک میشن و از پیشنهادهای tab-completion بازیکن هم حذف میشن.

### متن سه‌بعدی

```yml
text3d: true
text3d_note: "&b&lWelcome &f{player}\n&7Please log in or register"
text3d_scale: 1.5
text3d_distance: 2.5
text3d_height: 0.3
```

یه متن شناور جلوی بازیکن، تا وقتی داره لاگین میکنه. **فقط خود اون بازیکن میبینتش** و بعد از لاگین ناپدید میشه. `\n` = خط جدید و `{player}` = اسم بازیکن.

### اسم TAB

```yml
tab_names: true
tab_login_format: "LOGIN_USER_{number}"
tab_register_format: "REGISTER_USER_{number}"
```

وقتی بازیکن در حال لاگین/ثبت‌نامه، اسم توی لیست TAB عوض میشه تا اسم واقعیش نمایش داده نشه. `{number}` تعداد لاگین‌ها/ثبت‌نام‌هایی هست که **امروز** شروع شدن (هر روز صفر میشه و توی `counters.yml` ذخیره میشه).

### آخرین ورود و منطقه‌ی زمانی

```yml
last-login-format: "yyyy-MM-dd HH:mm:ss"
time-zone: ""
```

`last-login-format` از الگوهای [`DateTimeFormatter`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/format/DateTimeFormatter.html) جاوا پیروی میکنه. `time-zone` مقدارهایی مثل `Europe/Berlin` یا `Asia/Tehran` میگیره؛ خالی یعنی منطقه‌ی زمانی خود سرور.

### امنیت

```yml
security:
  min-password-length: 6
  max-password-length: 32
  max-login-attempts: 5          # 0 = unlimited
  login-timeout-seconds: 60      # 0 = no limit
  reminder-interval-seconds: 10  # 0 = off
```

### پیام‌ها

همه‌ی پیام‌هایی که به بازیکن نشون داده میشن زیر `messages:` هستن و آزادانه قابل تغییرن. placeholderهای موجود:

| Placeholder | کجا کار میکنه |
|---|---|
| `{prefix}` | هر پیامی (مقدار `messages.prefix`) |
| `{player}` | `must-register`، `must-login`، `wrong-name-kick`، `text3d_note` |
| `{name}` | `wrong-name-kick` (اسمی که اکانت با اون ثبت شده) |
| `{min}` / `{max}` | `password-too-short` / `password-too-long` |
| `{date}` | `last-login` |
| `{attempts}`، `{max}`، `{left}` | `wrong-password` |
| `{seconds}` | `timeout-kick` |
| `{player}`، `{count}` | `admin-deleted` (`{player}` توی `admin-not-found` هم کار میکنه) |
| `{count}`، `{players}` | `purge-list` (`{count}` توی `purge-done` هم کار میکنه) |

دستورهای ادمین از پیام‌های `no-permission`، `admin-usage`، `admin-not-found`، `admin-deleted`، `account-deleted-kick`، `purge-refused`، `purge-none`، `purge-list` و `purge-done` استفاده میکنن.

---

## نحوه‌ی کار

1. بازیکن وارد میشه و فوراً قفل میشه. در صورت تنظیم، به اتاق انتظار میره، از بقیه مخفی میشه و اسم موقت TAB میگیره.
2. اکانت به‌صورت **غیرهمزمان (async)** از ذخیره‌سازی خونده میشه، پس ترد اصلی سرور با MySQL یا دیسک بلاک نمیشه.
3. از بازیکن خواسته میشه `/register` (جدید) یا `/login` (قدیمی) بزنه.
4. هش و بررسی رمز هم async انجام میشن.
5. در صورت موفقیت، قفل برداشته میشه، بازیکن (در صورت تنظیم) تلپورت میشه و زمان آخرین ورودش ذخیره میشه.
6. رمز اشتباه زیاد یا دیر لاگین کردن باعث کیک شدن بازیکن میشه.

اگه ذخیره‌سازی خونده نشه، بازیکن با `messages.storage-error` کیک میشه و بدون محافظت وارد سرور نمیشه.

---

## فایل‌هایی که پلاگین میسازه

| فایل | کاربرد |
|---|---|
| `plugins/vkauth/configvk.yml` | تنظیمات و پیام‌ها |
| `plugins/vkauth/accounts.yml` | اکانت‌ها (وقتی ذخیره‌سازی YAML فعاله) |
| `plugins/vkauth/counters.yml` | شمارنده‌ی روزانه‌ی لاگین/ثبت‌نام برای اسم TAB |
| `plugins/vkauth/linked-accounts.yml` | چه کسایی توی آخرین بررسی اکانت داشتن (برای `online-rankvk`). ویرایشش نکن |

---

## بیلد

سورس یه پلاگین استاندارد Paper هست (`ir.gharri.vkauth`، کلاس اصلی `ir.gharri.vkauth.Vkauth`، `api-version: 1.21`). با Java 21 و Paper 1.21 API و Maven یا Gradle بیلدش کن و jar خروجی رو توی `plugins/` بذار.

---

## پشتیبانی

سؤال یا پیشنهادی داری؟ توی گیت‌هاب issue باز کن یا از طریق تلگرام به **BenyVK** پیام بده: <https://t.me/i36VK>

</div>
