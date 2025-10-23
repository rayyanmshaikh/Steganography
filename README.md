# Steganography Frontend

This is a Spring-Boot project deployed on [**AWS**](https://main.d245oznx2bab0g.amplifyapp.com/), with a front-end repo located [**here**](https://github.com/rayyanmshaikh/Steganography-Frontend). It allows you to:

- Upload an image (PNG, JPEG, JPG)
- Encode a secret text message into the image
- Download the encoded image as `encoded.png`
- Decode hidden text from a steganographic image

## Features
- Drag & drop image upload with preview
- Automatic calculation of maximum text capacity per image
- Encode text into images with a single click
- Decode hidden text from images
- Optional server base URL configuration for connecting to a backend running on a different host

## Backend Requirements
The frontend expects a running backend that provides the following endpoints:
- `POST /api/encodeTI` — Encodes text into an image and returns a PNG
- `POST /api/decodeTI` — Decodes hidden text from an image

Refer to the provided Spring Boot `StegController` for the backend implementation.

## Getting Started

1. Clone the repository:
   ```bash
   git clone <repo-url>
   cd steganography-frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm start
   ```

4. Open the app in your browser at `http://localhost:5173`

## Usage
1. Select or drag & drop an image.
2. Type the message you want to hide.
3. Click **Encode** to embed the text and download the resulting `encoded.png`.
4. To decode, upload an image with hidden text and click **Decode**.

## Notes
- Encoded images are always downloaded as `encoded.png`.
- Capacity is calculated client-side using the formula: `((width * height * 3) / 8) - 12`.
- Works with PNG, JPEG, and JPG images under 50 MB.
