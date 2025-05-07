## The Android project structure for MobileJarvisNative is organized as follows:

android/app/src/main/
├── java/
│   └── com/
│       └── cameronhightower/
│           └── mobilejarvisnative/
│               ├── MainApplication.java
│               ├── modules/
│               │   ├── auth/
│               │   │   ├── components/
│               │   │   ├── utils/
│               │   │   ├── services/
│               │   │   └── models/
│               │   ├── voice/
│               │   │   ├── components/
│               │   │   ├── utils/
│               │   │   ├── services/
│               │   │   └── models/
│               │   ├── wakeword/
│               │   │   ├── components/
│               │   │   ├── utils/
│               │   │   ├── services/
│               │   │   └── models/
│               │   ├── home/
│               │   │   ├── components/
│               │   │   ├── utils/
│               │   │   ├── services/
│               │   │   └── models/
│               │   └── settings/
│               │       ├── components/
│               │       ├── utils/
│               │       ├── services/
│               │       └── models/
│               └── utils/
│                   ├── common/
│                   ├── network/
│                   ├── storage/
│                   └── helpers/
├── res/
│   ├── layout/
│   ├── values/
│   ├── drawable/
│   └── mipmap/
├── assets/
└── AndroidManifest.xml 