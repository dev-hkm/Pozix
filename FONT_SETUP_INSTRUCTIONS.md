# Font Setup Instructions for Pozix

## Current Status
✅ The font system is **fully implemented and ready**  
❌ Custom fonts are **not yet installed** (only default font works)

## Why Custom Fonts Don't Work Yet

You added **ZIP files** containing fonts, but:
- **ZIP files cannot be used directly as Android fonts**
- Android requires `.ttf` or `.otf` files in `app/src/main/res/font/`
- Font files must have **lowercase names with underscores** (Android requirement)

## How to Add Custom Fonts

### Step 1: Extract Font Files from ZIP
1. Locate your ZIP font files
2. Extract the `.ttf` or `.otf` files from each ZIP
3. Rename them to lowercase with underscores:
   - ❌ Bad: `MyFont-Regular.ttf`, `Font Name.otf`
   - ✅ Good: `my_font_regular.ttf`, `font_name.otf`

### Step 2: Place Fonts in res/font Directory
Copy the extracted and renamed font files to:
```
app/src/main/res/font/
```

**Recommended file names:**
- `custom_font_1.ttf` (or `.otf`)
- `custom_font_2.ttf`
- `custom_font_3.ttf`

### Step 3: Uncomment Font Code

#### In `app/src/main/java/com/hkm/pozix/ui/theme/Type.kt`:
Find and uncomment these lines:
```kotlin
// val CustomFont1 = FontFamily(Font(R.font.custom_font_1))
// val CustomFont2 = FontFamily(Font(R.font.custom_font_2))
// val CustomFont3 = FontFamily(Font(R.font.custom_font_3))
```

And in the `getFontFamily` function:
```kotlin
// "custom1" -> CustomFont1
// "custom2" -> CustomFont2
// "custom3" -> CustomFont3
```

#### In `app/src/main/java/com/hkm/pozix/ui/screens/SettingsScreen.kt`:
Find the Font Section and uncomment the custom font cards (around line 90-110).

### Step 4: Sync and Build
1. Click **"Sync Project with Gradle Files"** in Android Studio
2. Build the app
3. Go to Settings → Font
4. You should now see and be able to select custom fonts!

## Example: Adding Google Fonts

If you downloaded fonts from Google Fonts:

1. **Extract from ZIP:**
   ```
   Roboto.zip → Roboto-Regular.ttf
   ```

2. **Rename:**
   ```
   Roboto-Regular.ttf → custom_font_1.ttf
   ```

3. **Copy to:**
   ```
   app/src/main/res/font/custom_font_1.ttf
   ```

4. **Uncomment code in Type.kt and SettingsScreen.kt**

5. **Sync & Build**

## Important Notes

- ⚠️ Font files MUST be lowercase with underscores
- ⚠️ Only `.ttf`, `.otf`, `.ttc`, and `.xml` are allowed in `res/font/`
- ⚠️ ZIP files cannot be used directly
- ✅ The app works perfectly with just the default font
- ✅ Custom fonts are optional but fully supported once added correctly

## Current Font System Features

✅ Font switching infrastructure is complete  
✅ Font selection persists across app restarts  
✅ Font applies to entire app typography  
✅ Graceful fallback to default if custom font missing  
✅ Works with English and Vietnamese  
✅ Dark mode compatible  

## Need Help?

If you're unsure which font files to use from your ZIPs, look for files ending in:
- `.ttf` (TrueType Font)
- `.otf` (OpenType Font)

Usually you want the "Regular" or "Normal" weight variant.
