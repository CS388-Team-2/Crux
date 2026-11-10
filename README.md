# Milestone 1 - Crux (Unit 7)

## Table of Contents

1. [Overview](#Overview)
1. [Product Spec](#Product-Spec)
1. [Wireframes](#Wireframes)

## Overview

### Description

An app that allows users to find nearby gyms, log their climbs in a diary where they can provide photos and videos of the climbs they attempted. The user can also keep their phone on them during the climb where sensors in the phone will be used to track the total distance they climbed during the session, and keep track of attempts made on a climb. They can also scroll through a feed of outdoor rock climbing areas or scroll through a feed of Kilter board setups that they can open to see the setting and try themselves.

### App Evaluation

- **Category:** Fitness
- **Mobile:** Uses pressure sensors and accelerometer to track height, camera to upload pictures and videos of the climbs, location to find nearby gyms.
- **Story:** Creates a better way for climbers to learn and enjoy the sport, providing a space for discovery, new challenges, and organized log of previous climbs to review and learn from
- **Market:** Rock climbers of any skill level
- **Habit:** Scroll through board climbs, save them for future sessions, and track your climbs
- **Scope:** Future scope could allow users to find outdoor climbs and also post beta of climbs to a feed of friends

## Product Spec

### 1. User Features (Required and Optional)

**Required Features**

1. Scrollable feed of Kilter board setups, where the user can tap a climb and see how the board is set
2. Logbook where users can log their day of climbing
3. Post pictures in the logbook
4. Track climbing distance using phone sensors when the phone is with the user during long climbs
5. Favorite Kilter board setups so they can be found easily

**Optional Features**

1. Find nearby rock climbing gyms using the user's current location
2. Find outdoor climbing puzzles
3. Post videos in the logbook

### 2. Screen Archetypes

Screens below match the [Climbing App Wireframe](https://www.figma.com/design/xuYsFaiOD8687KP2yTE25w/Climbing-App-Wireframe?node-id=0-1). Finding outdoor areas is still an optional feature and does not have a screen in this wireframe.

- **Welcome**
  - User sees what the app does and can get started
  - User can choose "I already have an account" (no sign-in screen is drawn yet)
- **Permissions**
  - User can allow location, so the feed can switch to the gym they walk into
  - User can allow motion and barometer, so the phone can count attempts and falls
  - User can allow notifications, so a session keeps running while the phone is locked
  - User can skip and go straight to the feed
- **Feed** (Stream)
  - User can scroll board climbs one at a time by swiping up
  - User can see the hold layout, name, grade, and angle
  - User can save a climb for later
  - User can open the full setting
  - User can see a banner when a session is already running
- **Filter Sheet**
  - User can filter the feed by board (Kilter, Tension, Moon), angle, and grade
  - User can hide climbs they have already sent
  - User can change which gym the feed is using
- **Gym Picker** (optional)
  - User can search gyms near their current location, sorted by distance
  - User can pick a gym and the feed switches to that gym's boards
  - User can star a gym so walking in can switch the feed and start a session
- **Route Detail** (Detail)
  - User can see the full hold layout, with start, hand, foot, and finish holds
  - User can see grade, angle, quality, board, and gym
  - User can save the climb
- **Saved** (Stream)
  - User can scroll the climbs they saved
  - User can filter that list by All, Sent, Not attempted, or Tried
  - User can open a saved climb to see its setting
- **Session** (Tracking)
  - User can track a session with the phone in their pocket
  - User can see elapsed time, attempts, tops, and distance climbed
  - User can log an attempt as Attempted or Topped, by hand or from the sensors
  - User can see each attempt, including ones caught automatically
  - User can end the session and turn it into a diary entry
- **Edit Attempt**
  - User can change an attempt's result, route, note, or video
  - User can record a new clip or pick one from the gallery
  - User can delete the attempt
- **Diary** (Stream)
  - User can scroll past days of climbing, with time, attempts, tops, distance, photos, and notes
  - User can start a new entry
- **Diary Entry** (Creation)
  - User can review the session totals: time, attempts, tops, and distance climbed
  - User can add photos (required) and notes (required)
  - User can attach a video clip (stretch)
  - User can save the day back to the diary

### 3. Navigation

**Tab Navigation** (Tab to Screen)

* **Feed** → Feed
* **Saved** → Saved
* **Session** → Session
* **Diary** → Diary

The Feed, Saved, and Diary screens also show a "Session active" banner that opens the Session screen.

**Flow Navigation** (Screen to Screen)

- Welcome
  - Get started → Permissions
  - I already have an account → sign-in is labeled, and no screen is drawn for it yet
- Permissions
  - Back → Welcome
  - Continue → Feed
  - Not now → Feed
- Feed
  - Swipe up → the next climb
  - Save → climb is added to Saved
  - Details → Route Detail
  - Gym chip or filter button → Filter Sheet
  - Session banner → Session
- Filter Sheet
  - Close, or tap outside the sheet → Feed
  - Change gym → Gym Picker
  - Show climbs → Feed, using the selected board, angle, and grade
- Gym Picker
  - Back → Filter Sheet
  - Tap a gym → Feed for that gym
- Route Detail
  - Back → Feed or Saved, whichever opened it
  - Save → climb is added to or removed from Saved
- Saved
  - Tap a climb → Route Detail
  - Session banner → Session
- Session
  - Recording → Record
  - Change → Saved, to pick which climb attempts are logged to
  - Tap an attempt → Edit Attempt
  - End session → Diary Entry, filled in with this session's time, attempts, tops, and distance
- Edit Attempt
  - Close or Save → Session
  - Change route → Saved
  - Replace or Record → Record
- Diary
  - New → Diary Entry
  - Tap a day → Diary Entry
  - Session banner → Session
- Diary Entry
  - Back → Diary
  - Save entry → Diary

## Digital Wireframes

Digital wireframes for each screen. The source file is the [Climbing App Wireframe](https://www.figma.com/design/xuYsFaiOD8687KP2yTE25w/Climbing-App-Wireframe?node-id=0-1).

**01 Welcome**

<img src="wireframes/01-welcome.png" width="280" alt="Welcome screen">

**02 Permissions**

<img src="wireframes/02-permissions.png" width="280" alt="Permissions screen">

**03 Feed**

<img src="wireframes/03-feed.png" width="280" alt="Feed screen">

**04 Feed, next climb**

<img src="wireframes/04-feed-next.png" width="280" alt="Next climb in the feed">

**05 Filter sheet**

<img src="wireframes/05-filter.png" width="280" alt="Filter sheet">

**06 Gym picker**

<img src="wireframes/06-gym-picker.png" width="280" alt="Gym picker">

**07 Route detail**

<img src="wireframes/07-route-detail.png" width="280" alt="Route detail">

**08 Saved**

<img src="wireframes/08-saved.png" width="280" alt="Saved climbs">

**09 Session**

<img src="wireframes/09-session.png" width="280" alt="Climbing session">

**10 Diary entry**

<img src="wireframes/10-diary-entry.png" width="280" alt="Diary entry">

**11 Diary**

<img src="wireframes/11-diary.png" width="280" alt="Diary list">

**12 Record**

<img src="wireframes/12-record.png" width="280" alt="Record an attempt">

**13 Edit attempt**

<img src="wireframes/13-edit-attempt.png" width="280" alt="Edit an attempt">

<br>

### Interactive Prototype Video
<img width="455" height="981" alt="CruxWireframe" src="https://github.com/user-attachments/assets/07329ae0-fded-441d-a463-e6a290645d5b" />


<br>

# Milestone 2 - Build Sprint 1 (Unit 8)

## GitHub Project board

Project: [CS388 Team 2 — Crux board](https://github.com/orgs/CS388-Team-2/projects)

<img width="1895" height="721" alt="image" src="https://github.com/user-attachments/assets/c2a11e98-19a6-43d9-ad3c-643c596ba152" />

## Issue cards

Sprint 1 (Project Setup) cards:

<img width="1910" height="727" alt="image" src="https://github.com/user-attachments/assets/23b02fa5-7293-4ec5-9b5f-71b2f0c638a8" />


Next sprint (Milestone 2 on GitHub: _Account creation and settings, Scrollable feed, climb logging_) cards:

<img src="docs/milestone2-board-sprint2.png" width="600" alt="Next sprint issues with assignees">

## Issues worked on this sprint

| Issue | Title | Owner | Sprint 1 outcome |
|-------|-------|-------|------------------|
| [#5](https://github.com/CS388-Team-2/Crux/issues/5) | Create Manifest | mFerr16 | Manifest declares `INTERNET`, location, camera, activity recognition, notifications |
| [#3](https://github.com/CS388-Team-2/Crux/issues/3) | Initial Permissions | alexismulvany | Permissions screen + runtime requests; Skip / Continue → Feed |

_No other Project Setup issues existed on GitHub this sprint; see “Next sprint” below for DB and full UI skeleton work._

## Build progress (Sprint 1)

- Android Studio project builds with Jetpack Compose + Navigation.
- **Welcome → Permissions → stub Feed** matches wireframes 01–03 at a skeleton level (not pixel-perfect).
- Room, auth, scrollable feed, favorites, diary, and sensors are **not** implemented yet (planned for GitHub Milestones 2 and 3).

### Demo GIF

Record in Android Studio (**View → Tool Windows → Logcat** area, or device **Screen record**), then export and place at `docs/milestone2-demo.gif`:

<img src="docs/milestone2-demo.gif" width="300" alt="Welcome, permissions flow, and stub Kilter feed">

_Suggested recording flow (≈15–25 s):_ launch app → **Get started** → **Allow permissions** (optional) → **Continue to feed** → scroll stub climb cards.

## User stories / features (Sprint 1)

From the [product spec](#1-user-features-required-and-optional) — only items touched this sprint:

- [x] App shell and onboarding path (Welcome, Permissions) toward the feed
- [x] Declare and request permissions needed for later features (location, motion, camera, notifications)
- [ ] Scrollable Kilter board feed + route detail
- [ ] Logbook / diary
- [ ] Photos in logbook
- [ ] Sensor-based climb distance
- [ ] Favorite board setups

<br>

# Milestone 3 - Build Sprint 2 (Unit 9)

## GitHub Project board

[Add screenshot of your Project Board with the updated status of issues for Milestone 3. Note that these should include the updated issues you worked on for this sprint and not be a duplicate of Milestone 2 Project board.] <img src="YOUR_WIREFRAME_IMAGE_URL" width=600>

## Completed user stories

- List the completed user stories from this unit
- List any pending user stories / any user stories you decided to cut
from the original requirements

[Add video/gif of your current application that shows build progress]
<img src="YOUR_WIREFRAME_IMAGE_URL" width=600>

## App Demo Video

- Embed the YouTube/Vimeo link of your Completed Demo Day prep video
