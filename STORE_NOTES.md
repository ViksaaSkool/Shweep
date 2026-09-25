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
    • Choose white or black sheep for free — black sheep brings a full night theme
    • Unlock Colorful Sheep for a rainbow flock
    • Change your choice anytime in Settings
    • Invite friends to swap doom-scrolling for sheep
    
    🐏 FREE COUNTING & OPTIONAL UPGRADES
    Count up to 35 sheep for free. Once you use your allowance, it resets 24 hours later.
    
    Want to keep counting without the wait? Unlock Unlimited Sheep with an optional one-time in-app purchase.
    
    Want a rainbow flock? Unlock Colorful Sheep with a separate optional one-time purchase.
    
    There are no subscriptions, and the localized price is shown before you buy.
    
    🔒 PRIVATE BY DESIGN
    • No account and no login
    • No ads
    • Session history stays in your device's app storage and may be included in platform backups
    • RevenueCat processes purchase history and purchase-related identifiers to manage and restore purchases; see our Privacy Policy
    • Counting works offline. Buying, restoring, and checking purchase status require an internet connection
    
    One gentle app. One small flock. One softer way to end the day.
    
    Download Shweep and go to sleep like your ancestors did.
    
    Shweep is a general-wellness app. It is not a medical device and does not diagnose, treat, cure, or prevent any medical condition. Consult a qualified healthcare professional for medical advice, diagnosis, or treatment.
    
    Support: contact@viktorarsovski.xyz
    Privacy Policy: https://shweep.lol/privacy
    Terms of Service: https://shweep.lol/terms
```

### Play one-time products

Paste these into **Monetize with Play → Products → In-app products** (one-time products).

```
Product ID: unlimited_sheep
Title: Unlimited Sheep
Description: Remove the 35-sheep allowance and count without waiting for its 24-hour reset.
```

```
Product ID: colorful_sheep
Title: Colorful Sheep
Description: Unlock the rainbow sheep color. Unlimited counting is sold separately.
```

- Create both as one-time managed products, multi-quantity disabled, base price ~$0.99 (let Play auto-localize), active, and available everywhere the app ships.
- Product IDs cannot be renamed or reused; confirm them before creating.

### Play screenshot captions

Optional short captions for the 2.0 screenshots. Do not put prices, discounts, "permanent", or subscription words on any asset.

```
Choose your flock
Unlock rainbow sheep
Optional one-time upgrades
Count 35 sheep free
```

Recommended shots to add or replace: the color picker with white, black, and locked rainbow; rainbow sheep in the counting scene; Settings → Upgrades showing both independent upgrades; the allowance dialog or a tasteful 35-sheep graphic.

### Play Console paste checklist

Manual, outside the repository:

- [ ] Store listing → Full description (Google Play section above)
- [ ] Store listing → What's new (release notes below)
- [ ] Store listing → screenshots and captions
- [ ] Monetize → In-app products → create `unlimited_sheep` and `colorful_sheep`
- [ ] App content → Data safety (purchase history collected, encrypted in transit, app functionality + analytics)
- [ ] App content → Health apps declaration (Sleep Management; Stress Management, Relaxation, Mental Acuity)
- [ ] App content → Ads: No
- [ ] Store listing → Privacy Policy URL: https://shweep.lol/privacy/
- [ ] Confirm the uploaded bundle contains `com.android.vending.BILLING`

## App Store

### Subtitle (23/30)

```
Swipe sheep. Wind down.
```

### Promotional text (131/170, optional)

```
White, black, or rainbow sheep, a grayscale wind-down, and locally stored session history — your whole wind-down in one gentle app.
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
• White or black sheep for free — black sheep switches the whole app to a night look
• Unlock Colorful Sheep for a rainbow flock
• Pick your sidekick on first launch, change it anytime in Settings
• Invite friends to trade doom-scrolling for sheep

FREE COUNTING & OPTIONAL UPGRADES
Count up to 35 sheep for free. Once you use your allowance, it resets 24 hours later.

Want to keep counting without the wait? Unlock Unlimited Sheep with an optional one-time in-app purchase.

Want a rainbow flock? Unlock Colorful Sheep with a separate optional one-time purchase.

There are no subscriptions, and the localized price is shown before you buy.

PRIVATE BY DESIGN
• No account and no login
• No ads
• Session history stays in your device's app storage and may be included in platform backups
• RevenueCat processes purchase history and purchase-related identifiers to manage and restore purchases; see our Privacy Policy
• Counting works offline. Buying, restoring, and checking purchase status require an internet connection

One gentle app. One small flock. One softer way to end the day.

Support: contact@viktorarsovski.xyz
Privacy Policy: https://shweep.lol/privacy
Terms of Service: https://shweep.lol/terms
```

### In-app purchases

Create both as **non-consumables** in App Store Connect (Monetization → In-App Purchases). Product IDs cannot be changed after saving.

```
Reference Name: Unlimited Sheep
Product ID: unlimited_sheep
Type: Non-Consumable
Display Name (≤30): Unlimited Sheep
Description (≤45): Remove the 35-sheep allowance.
```

```
Reference Name: Colorful Sheep
Product ID: colorful_sheep
Type: Non-Consumable
Display Name (≤30): Colorful Sheep
Description (≤45): Unlock the rainbow sheep color.
```

- Price each at the ~$0.99 tier. Leave them "Ready to Submit" so they are reviewed with the next binary.
- Add a review screenshot and a review note for each.

### App Store paste checklist

Manual, outside the repository:

- [ ] Version metadata → Description
- [ ] Version metadata → Promotional text
- [ ] Version metadata → What's New (release notes below)
- [ ] Monetization → In-App Purchases → create `unlimited_sheep` and `colorful_sheep` as non-consumables
- [ ] App Privacy → declare Identifiers (device ID) and Usage Data (product interaction) for RevenueCat
- [ ] Privacy Policy URL: https://shweep.lol/privacy/

## Release notes (2.0.0)

```
Shweep 2.0 introduces a free 35-sheep allowance that resets after 24 hours, plus two optional one-time upgrades: Unlimited Sheep and Colorful Sheep. We've also updated Settings, our Privacy Policy, and our Terms to explain purchase processing.
```

## Version boundary

- **1.0.0 / 1.0.2** — no sheep allowance and no in-app purchase. Settings → About shows the installed version. These versions keep their existing behavior; the 2.0 changes apply only after a user updates.
- **2.0.0** — introduces the 35-sheep allowance, two independent optional one-time purchases ("Unlimited Sheep" and "Colorful Sheep"), and a one-time "what's changed" notice on first launch.
- Advertise the allowance and the purchases only in listings for 2.0.0 and later. Do not promise unrestricted counting in copy that also ships to 1.0.x.
- Never hard-code a price in the descriptions; the store localizes the price, and the App shows the localized price before purchase.

## Notes on choices

- Reused the app's established voice: "go to sleep like your ancestors", "Count sheep. Quiet your mind." (Play screenshot 05), and "Session history" (the app's History-screen term).
- Accuracy fixes: "soothing stories" removed (Session history is counting-session history, not bedtime stories); "average time to sleep" replaced with "average wind-down time"; no claim that Shweep detects sleep or knows when the user drifts off. The app records elapsed counting time and ends a session when the app is backgrounded for about 10 seconds or the phone locks.
- "No ads" and "no account" claims remain accurate and are backed by docs/privacy. The earlier blanket "no analytics / no tracking / on-device" claim was removed from the store copy because RevenueCat processes purchase information; the copy now describes that processing accurately and still states that session history stays local.
- Removed "insomnia" and "sleep aid" from store keywords to avoid implying treatment of a sleep disorder; keywords stay general-wellness (night routine, calm, relax, unwind).
- In-app purchases: Shweep ships a 35-sheep allowance and two independent optional one-time purchases, "Unlimited Sheep" and "Colorful Sheep" (base price $0.99 each, localized by each store), starting with version 2.0.0. Both are behind `FeatureFlags.LIMITED_DAILY_SHEEP_ENABLED` and must only be enabled once the RevenueCat project, the Play/App Store products, and the public SDK keys exist. The listings above already disclose the allowance and both purchases. The Play Data safety form and the App Privacy labels must be updated before enabling. See MONETIZATION_SETUP.md.
- Store privacy copy no longer claims "no analytics, no tracking" as a blanket statement: RevenueCat processes a device-generated identifier and purchase information for purchase management, which must be disclosed.
- Two independent one-time products now ship: **Unlimited Sheep** (`unlimited_sheep`) and **Colorful Sheep** (`colorful_sheep`). They are separate RevenueCat entitlements — buying one never unlocks the other — and both must be disclosed in the listing and store privacy forms.
- App Store copy avoids emoji-heavy bullets (plain headers match the dreamy minimal tone) and avoids any Android references.
- Feature graphic sentence should read: "Gentle wind-down sessions, nightly insights, and a dreamy little flock for bedtime." (replaces the old "soothing stories" line).
