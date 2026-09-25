# Store Listing Notes

Store listing copy for Shweep, derived from the app's own resources (README, `Strings.kt`, STORE_DESIGNS.md taglines, shweep.lol landing copy, and docs/privacy). All field limits verified.

Not committed — working reference for Play Console / App Store Connect entry.

Health declaration: this listing markets Shweep as a general-wellness wind-down app. In Play Console, declare **Sleep Management** and **Stress Management, Relaxation, Mental Acuity** (the copy uses "calm" and "quiet your mind"). Do not select "My app doesn't provide any health features." Shweep is not a medical device and does not detect sleep.

## Google Play

### Short description (73/80)

```
Swipe sheep, slow down, and make counting part of your bedtime wind-down.
```

### Full description

```
    A gentler way to wind down.
    
    Shweep (sheep • sleep • swipe) turns the classic sheep-counting ritual into a gentle bedtime wind-down. Instead of doom-scrolling until 2 a.m., swipe a soft little flock into a moonlit meadow and let the day drift away — literally.
    
    🛌 COUNT SHEEP, QUIET YOUR MIND
    • Swipe up to flick a sheep into the meadow, or drag one wherever you like
    • Sheep wander in a lazy zig-zag, bump softly into each other, and drift away after a while so the meadow never overcrowds
    • Every sheep walks on animated legs — a tiny living flock, not a static counter
    
    🌗 A WIND-DOWN THAT FOLLOWS YOU
    As your swiping slows, the scene gradually fades to grayscale. One more swipe and the color returns. If you leave the app or lock your phone for about 10 seconds, Shweep records the completed counting session — no timers to set, nothing to turn off.
    
    📖 SESSION HISTORY
    Review your previous counting sessions and summary statistics.
    • Your average wind-down time, at a glance
    • Per-session cards: when you started, how long you counted, and how many sheep you counted
    • A mini flock visual in your session summary
    
    🐑 MAKE BEDTIME YOURS
    • Choose white or black sheep — black sheep brings a full night theme
    • Change your mind anytime in Settings
    • Invite friends to swap doom-scrolling for sheep
    
    🐏 FREE COUNTING & OPTIONAL UPGRADES
    Count up to 35 sheep for free. Once you use your allowance, it resets 24 hours later. Want to keep counting without the wait? Unlock Unlimited Sheep with an optional one-time in-app purchase. Want a rainbow flock instead? Unlock Colorful Sheep too. Both are one-time purchases — no subscription — and the price is shown before you buy.
    
    🔒 PRIVATE BY DESIGN
    • No account and no login
    • No ads
    • Session history stays in your device's app storage and may be included in platform backups
    • RevenueCat manages purchase availability and verification; see our Privacy Policy
    • Counting works offline. Purchase services — buying, restoring, and checking purchase status — use an internet connection
    
    One gentle app. One small flock. One softer way to end the day.
    
    Download Shweep and go to sleep like your ancestors did.
    
    Shweep is a general-wellness app. It is not a medical device and does not diagnose, treat, cure, or prevent any medical condition. Consult a qualified healthcare professional for medical advice, diagnosis, or treatment.
    
    Support: contact@viktorarsovski.xyz
    Privacy Policy: https://shweep.lol/privacy
    Terms of Service: https://shweep.lol/terms
```

## App Store

### Subtitle (23/30)

```
Swipe sheep. Wind down.
```

### Promotional text (112/170, optional)

```
Black sheep, a grayscale wind-down, and locally stored session history — your whole wind-down in one gentle app.
```

### Keywords field (91/100)

```
sheep,sleep,counting sheep,bedtime,night routine,calm,relax,wind down,doom scrolling,unwind
```

### Description

```
Sheep • Sleep • Swipe

Shweep turns the classic sheep-counting ritual into a gentle bedtime wind-down for your phone. Instead of doom-scrolling until 2 a.m., flick a soft little flock into a moonlit meadow and let the day drift away — literally.

Count sheep, quiet your mind.

HOW IT WORKS
• Swipe up to send a sheep into the meadow, or drag one exactly where you want it
• Sheep wander in a lazy zig-zag, bump softly into each other, and drift away after a while so the meadow never overcrowds
• Every sheep walks on animated legs — a tiny living flock, not a static number

A WIND-DOWN THAT FOLLOWS YOU
As your swiping slows, the scene gradually fades to grayscale. One more swipe and the color returns. If you leave the app or lock your phone for about 10 seconds, Shweep records the completed counting session — no timers, nothing to switch off.

SESSION HISTORY
Review your previous counting sessions and summary statistics.
• Average wind-down time at a glance
• Per-session cards: when you started, how long you counted, how many sheep you counted
• A mini flock visual in your session summary

MAKE BEDTIME YOURS
• White or black sheep — black sheep switches the whole app to a night look
• Pick your sidekick on first launch, change it anytime in Settings
• Invite friends to trade doom-scrolling for sheep

FREE COUNTING & OPTIONAL UPGRADES
Count up to 35 sheep for free. Once you use your allowance, it resets 24 hours later. Want to keep counting without the wait? Unlock Unlimited Sheep with an optional one-time in-app purchase. Want a rainbow flock instead? Unlock Colorful Sheep too. Both are one-time purchases — no subscription — and the price is shown before you buy.

PRIVATE BY DESIGN
• No account and no login
• No ads
• Session history stays in your device's app storage and may be included in platform backups
• RevenueCat manages purchase availability and verification; see our Privacy Policy

One gentle app. One small flock. One softer way to end the day.

Support: contact@viktorarsovski.xyz
Privacy Policy: https://shweep.lol/privacy
Terms of Service: https://shweep.lol/terms
```

## Release notes (2.0.0)

```
This update introduces a free 35-sheep allowance, which resets 24 hours after it is used, and an optional one-time Unlimited Sheep purchase. We've also updated our Privacy Policy and Terms to explain purchase processing.
```

## Version boundary

- **1.0.0 / 1.0.2** — no sheep allowance and no in-app purchase. Settings → About shows the installed version.
- **2.0.0** — introduces the 35-sheep allowance, the optional one-time "Unlimited Sheep" purchase, and a one-time "what's changed" notice on first launch.
- Advertise the allowance and the purchase only in listings for 2.0.0 and later. Do not promise unrestricted counting in copy that also ships to 1.0.x.
- Never hard-code a price in the descriptions; the store localizes the price, and the App shows the localized price before purchase.

## Notes on choices

- Reused the app's established voice: "go to sleep like your ancestors", "Count sheep. Quiet your mind." (Play screenshot 05), and "Session history" (the app's History-screen term).
- Accuracy fixes: "soothing stories" removed (Session history is counting-session history, not bedtime stories); "average time to sleep" replaced with "average wind-down time"; no claim that Shweep detects sleep or knows when the user drifts off. The app records elapsed counting time and ends a session when the app is backgrounded for about 10 seconds or the phone locks.
- "No ads / no analytics / no tracking / on-device" claims are backed by docs/privacy — safe for the Play Data Safety and App Privacy forms.
- Removed "insomnia" and "sleep aid" from store keywords to avoid implying treatment of a sleep disorder; keywords stay general-wellness (night routine, calm, relax, unwind).
- In-app purchases: Shweep ships a 35-sheep allowance and an optional one-time "Unlimited Sheep" purchase (base price $0.99, localized by each store) starting with version 2.0.0. The feature is behind `FeatureFlags.LIMITED_DAILY_SHEEP_ENABLED` and must only be enabled once the RevenueCat project, the Play/App Store products, and the public SDK keys exist. The listings above already disclose the allowance and the purchase. The Play Data safety form and the App Privacy labels must be updated before enabling. See MONETIZATION_SETUP.md.
- Store privacy copy no longer claims "no analytics, no tracking" as a blanket statement: RevenueCat processes a device-generated identifier and purchase information for purchase management, which must be disclosed.
- Two independent one-time products now ship: **Unlimited Sheep** (`unlimited_sheep`) and **Colorful Sheep** (`colorful_sheep`). They are separate RevenueCat entitlements — buying one never unlocks the other — and both must be disclosed in the listing and store privacy forms.
- App Store copy avoids emoji-heavy bullets (plain headers match the dreamy minimal tone) and avoids any Android references.
- Feature graphic sentence should read: "Gentle wind-down sessions, nightly insights, and a dreamy little flock for bedtime." (replaces the old "soothing stories" line).
