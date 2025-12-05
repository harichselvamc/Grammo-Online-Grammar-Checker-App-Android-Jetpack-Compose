# Grammo – Grammar Checker App (Android + Jetpack Compose)

Grammo is a simple English grammar-checking app for Android.  
It uses the LanguageTool online API to detect grammar and spelling mistakes, highlight them, and show suggestions.  
The UI is built with Jetpack Compose for a clean and modern experience.

---

## Download

If you want to try the app:

1. Go to the **Releases** section of this repository (right side or top menu in GitHub).  
2. Open the latest release (for example: `v1.0.0`).  
3. Download the first `.apk` file attached (for example: `app-debug.apk`).  
4. Copy it to your Android device and install it (you may need to allow installation from unknown sources).

---
## Features

- Grammar and spelling check using LanguageTool API  
- Auto-fix toggle (automatically applies the first suggestion)  
- Error highlighting in the text  
- Tap-to-fix correction suggestions  
- Word and character counter  
- Simple and modern Compose UI  

---

## How It Works

1. Type or paste your text  
2. Tap "Check Grammar"  
3. Grammo analyzes your text and shows corrections  
4. With Auto-fix ON, the app automatically fixes mistakes  
5. With Auto-fix OFF, you choose corrections manually through tap-to-fix  

---

## Tech Used

- **Kotlin**  
  The main programming language used to develop the app. Modern, clean, and designed for Android.

- **Jetpack Compose**  
  Android’s modern UI toolkit. Used to build the entire UI with less code and better structure.

- **Coroutines**  
  Handles background work such as API calls without freezing the user interface. Keeps the app responsive.

- **LanguageTool API**  
  The online grammar-checking service. Grammo sends text to LanguageTool and receives error details and correction suggestions.

---

## Status

This is an early version of Grammo.  
More features and improvements will be added in future updates.


---

## Contributions

Pull requests and suggestions are welcome.

