# 颜色乐园运行截图

截图来自安装调试 APK 后运行的 Android 14 / API 34 模拟器，横屏分辨率 1920 × 1200。

| 页面 | 截图 |
| --- | --- |
| 五入口首页 | [首页](01-首页.png) |
| 颜色组与活动入口 | [颜色乐园](02-颜色乐园.png)、[游戏选择](04-游戏选择.png) |
| 色块与认读 | [认识颜色](03-认识颜色.png) |
| 三种游戏 | [同色配对](05-同色配对.png)、[听音找色](06-听音找色.png)、[颜色小画家](07-颜色小画家.png) |
| 奖励与设置 | [上色完成](08-上色完成.png)、[颜色贴纸](09-颜色贴纸.png)、[家长设置](10-家长设置.png) |

复拍可在安装应用及测试 APK 后运行 `adb shell am instrument -w -e class com.guguabc.kids.ColorFlowTest -e colorShots true com.guguabc.kids.test/androidx.test.runner.AndroidJUnitRunner`。图片位于设备的 `/sdcard/Android/data/com.guguabc.kids/files/`。
