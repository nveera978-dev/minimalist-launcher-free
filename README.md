# Minimalist Launcher Free (Digital Detox & Brain Utilization)

A 100% free, lightweight, privacy-friendly alternative to commercial subscription-based minimalist launchers (like Minimalist Phone Launcher).

---

## 🌟 Why This Exists
Commercial minimalist launchers charge expensive subscriptions ($30/year or recurring fees) for basic text screens. 

This project gives you **all the same dopamine-reduction and focus tools completely free and open source**:
1. **Monochrome Text-Only Home Screen**: No colorful badges, app icons, or visual stimulation designed by tech companies to trigger dopamine cravings.
2. **Mindful Breathing Delay (The "Brain Wakeup" Feature)**:
   - When you tap entertainment apps like YouTube or Instagram, a 5-second breathing pause appears.
   - It interrupts the unconscious "muscle-memory" reflex.
   - You can tap *"I don't need this (Exit to Home)"* — saving precious time immediately.
3. **Session Time Limiter**:
   - Set an intentional 5 or 10-minute session limit before opening addictive apps.
4. **Mindful Exit Reminder ("Time is Up!")**:
   - When your session concludes, a full-screen notification interrupts: *"Time is up! Use your brain now to exit and utilize your time in real life."*
5. **Alphabetical A-Z App Drawer**:
   - Fast instant search to find utility apps without mindless browsing.
6. **Time Saved Dashboard**:
   - Tracks how many mindless impulse opens you successfully resisted today.

---

## 🎮 How to Test Right Now (Interactive Simulator)
Before building the Android APK, you can open and test the complete interactive simulator directly in your browser:

Open file:
`minimalist_launcher_simulator.html`

In this simulator, you can:
- Tap **YouTube** or **Instagram** to experience the 5-second mindful breathing pause.
- Tap **"I don't need this (Exit)"** and see your resisted impulse counter increment.
- Test the intentional **Session Timer** and trigger the **"Time is Up!"** brain alert.
- Switch themes (Pure OLED Black, Slate, Forest, Paper).
- Search the A-Z app drawer.

---

## 📱 Getting the Real App on Your Android Phone

### Option A: Free 1-Click Cloud Build (No Android Studio Needed!)
Since you are not a developer and don't need to install 15 GB of Android Studio:
1. Create a free account on [GitHub.com](https://github.com).
2. Create a new private or public repository.
3. Upload the files from this `minimalist_phone_launcher` folder into your repository.
4. GitHub Actions (already configured in `.github/workflows/build-apk.yml`) will automatically run in the cloud.
5. Go to the **Actions** tab on your GitHub repository, click the latest build, and download `MinimalistLauncher-Free-APK.zip`.
6. Extract and install `app-debug.apk` directly on your Android phone!

### Option B: Local Android Studio Build
If you or a friend have Android Studio:
1. Open Android Studio -> **Open an Existing Project** -> select `minimalist_phone_launcher`.
2. Let Gradle sync for 1 minute.
3. Plug in your Android phone via USB (with USB Debugging turned on) and click the green **Play (Run)** button.

---

## 🔒 Privacy & Battery
- **0 Trackers, 0 Analytics, 0 Ads**: Everything runs locally on your device.
- **Pure AMOLED Black**: Saves substantial battery life on OLED/AMOLED screens.
- **Under 5 MB**: Ultra lightweight compared to commercial apps.
