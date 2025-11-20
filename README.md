# Pixel Painter

Pixel Painter is an Android application that converts photos into a simplified, color‑coded grid so users may enjoy a guided coloring activity.

## Overview

This app allows users to upload a photo, automatically create a simplified pixelated verison, generate numbered paint regions, and color them in-app.

## Features

* **Curated Images** - Select a famous piece of artwork
* **Photo Importing** – Select images from local camera roll 
* **Custom Painting Image Processor** – Converts images into a paint-by-number palette using k-means palette reduction.
* **Interactive Coloring Mode** – Tap regions of the grid to fill them with their correct color.
* **Zoom & Pan** – Switch between paint mode and zoom mode to navigate easily.
* **Share Options** – View the completed painting or export it and share with a friend.

## Getting Started

### Prerequisites

* Android Studio 
* Android SDK and emulator or device

### Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/SeanHordines/paint_by_number.git
   ```
2. Open the project in **Android Studio**.
3. Allow Gradle to sync.
4. Run the app on an emulator or physical Android device.

## How It Works

1. User selects either a custom image from their camera roll or a pre-loaded image. 
2. App applies a image processor to derive a limited pixel grid and color palette.
3. Segmented regions are identified and assigned a number.
4. The user colors regions manually using an interactive canvas.
5. Finished artwork can be viewed and shared.

## Technologies Used

* Android Studio
* Java
* XML
* Android Jetpack Libraries 
* Bitmap processing utilities
* Custom image processor
* Custom drawing canvas

## Contact

For ideas, questions, or collaboration, reach out through GitHub to contact Sean Hordines, Ian Holbrook, Kaley Hague or Joshua Garcia.
