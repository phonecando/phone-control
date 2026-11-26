# Third-party notices

Phone control uses these components. Each keeps its own license, and nothing in
our [`LICENSE`](LICENSE) restricts what that license lets you do with it.
PhoneCanDo is an independent project and is not affiliated with or endorsed by
any of the projects below.

Phone control itself is Copyright (c) 2026 Krishna Kushwaha, under the
PhoneCanDo Source-Available License ([`LICENSE`](LICENSE)). The PhoneCanDo app,
which ships it, carries its own, longer notices for everything in the app
(<https://phonecando.app/licenses/>).

`tools/` is plain TypeScript with no dependencies. The components below are
what the engine (`engine/`) builds with.

## Kotlin, kotlinx.coroutines and kotlinx.serialization — Apache License 2.0

The engine is written in Kotlin and uses the Kotlin standard library,
kotlinx.coroutines (`kotlinx-coroutines-core` and `kotlinx-coroutines-android`
1.10.2) and kotlinx.serialization (`kotlinx-serialization-json` 1.8.1, with the
Kotlin serialization compiler plugin), at the versions its `build.gradle.kts`
files pin. Apache License 2.0. Copyright 2010-2024 JetBrains s.r.o. and Kotlin
Programming Language contributors (Kotlin); Copyright 2000-2020 JetBrains s.r.o.
and Kotlin Programming Language contributors (kotlinx.coroutines); Copyright
2017-2024 JetBrains s.r.o. (kotlinx.serialization). The license text is at
<https://www.apache.org/licenses/LICENSE-2.0>.

## Shizuku API — MIT License

The optional `shizuku` module (`engine/shizuku/`) uses
[Shizuku API](https://github.com/RikkaApps/Shizuku-API) (`dev.rikka.shizuku:api`
and `dev.rikka.shizuku:aidl` 13.1.5). MIT License, Copyright (c) 2021 RikkaW.
It talks to the [Shizuku](https://github.com/RikkaApps/Shizuku) app, which you
install yourself and which keeps its own license. The PhoneCanDo app does not
bundle this module.

## Gradle Wrapper — Apache License 2.0

`engine/gradlew`, `engine/gradlew.bat` and `engine/gradle/wrapper/` are the
[Gradle](https://gradle.org) Wrapper, which downloads the Gradle version
`gradle-wrapper.properties` names. Apache License 2.0. Copyright © 2015 the
original authors. Our copy of `gradlew.bat` came by way of React Native's
template and keeps its header too: MIT License, Copyright (c) Meta Platforms,
Inc. and affiliates.

## Build and test tools

Building and testing the engine also fetches tools that are not part of what
it ships: the Android Gradle Plugin (Apache License 2.0), JUnit 5 (Eclipse
Public License 2.0) and JUnit 4 (Eclipse Public License 1.0). Each keeps its
own license, in its own package.
