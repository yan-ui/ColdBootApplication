# ColdBootApplication
Android 启动优化之冷启动
1.应用的启动方式

通常来说，启动方式分为两种：冷启动和热启动。

1、冷启动：当启动应用时，后台没有该应用的进程，这时系统会重新创建一个新的进程分配给该应用，这个启动方式就是冷启动。

2、热启动：当启动应用时，后台已有该应用的进程（例：按back键、home键，应用虽然会退出，但是该应用的进程是依然会保留在后台，可进入任务列表查看），所以在已有进程的情况下，这种启动会从已有的进程中来启动应用，这个方式叫热启动。 

2App的启动过程

本文所指的优化针对冷启动。简单解释一下App的启动过程：

1.点击Launcher，启动程序,通知ActivityManagerService
2.ActivityManagerService通知zygote进程孵化出应用进程，分配内存空间等
3.执行该应用ActivityThread的main()方法
4.应用程序通知ActivityManagerService它已经启动，ActivityManagerService保存一个该应用的代理对象,ActivityManagerService通过它可以控制应用进程
5.ActivityManagerService通知应用进程创建入口的Activity实例，执行它的生命周期

启动过程中Application和入口Activity的生命周期方法按如下顺序调用：

1.Application 构造方法
2.attachBaseContext()
3.onCreate()
4.入口Activity的对象构造
5.setTheme() 设置主题等信息
6.入口Activity的onCreate()
7.入口Activity的onStart()
8.入口Activity的onResume()
9.入口Activity的onAttachToWindow()
10.入口Activity的onWindowFocusChanged()



3启动时间统计

理论上来说当回调到入口Activity的onResume（）方法时，App就算正式启动了，但是从这种意义上其实是不严谨的，因为在这个时间点
实际只完成了主题信息以及view树建立等。

而view绘制的真正过程measure layout draw过程并没有，所以不是真正意义上的“可见”。 在onWindowFocusChanged()回调处更接近事实。

所以我们确认启动时间可以从Application的构造方法记录开始时间，然后到入口Activity的onWindowFocusChanged()再记录一个时间，两者之差就是App的启动时间。

当然这是比较笨的方法，adb命令里有相应的记录命令 ：adb shell am start -W 包名/入口类全路径名
adb shell am start -W [PackageName]/[PackageName.MainActivity]
执行成功后将返回三个测量到的时间，这里面涉及到三个时间，ThisTime、TotalTime 和 WaitTime。

WaitTime 是 startActivityAndWait 这个方法的调用耗时;
ThisTime 是指调用过程中最后一个 Activity 启动时间到这个 Activity 的 startActivityAndWait 调用结束;
TotalTime 是指调用过程中第一个 Activity 的启动时间到最后一个 Activity 的 startActivityAndWait 结束。

如果过程中只有一个 Activity ，则 TotalTime 等于 ThisTime。

利用TraceView分析启动时间

在onCreate开始和结尾打上trace.
Debug.startMethodTracing("TestApp");...Debug.stopMethodTracing();
运行程序, 会在sdcard上生成一个”TestApp.trace”的文件. 

注意: 需要给程序加上写存储的权限:
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
通过adb pull将其导出到本地
adb pull /sdcard/TestApp.trace ~/testSpeed.trace
打开DDMS分析trace文件,展开后，大多数有以下两个类别:

Parents:调用该方法的父类方法 
Children:该方法调用的子类方法 

如果该方法含有递归调用，可能还会多出两个类别: 

Parents while recursive:递归调用时所涉及的父类方法 
Children while recursive:递归调用时所涉及的子类方法

开发者最关心的数据有： 

很重要的指标：Calls + Recur Calls / Total , 最重要的指标： Cpu Time / Call 
因为我们最关心的有两点，一是调用次数不多，但每次调用却需要花费很长时间的函数。这个可以从Cpu Time / Call反映出来。

另外一个是那些自身占用时间不长，但调用却非常频繁的函数。这个可以从Calls + Recur Calls / Total 反映出来。

然后我们可以通过这样的分析，来查看启动的时候，哪些进行了耗时操作，然后进行相应的处理，比如能不在主线程中做的，放入子线程中，还有一些懒加载处理，比如有些Application中做了支付SDK的初始化，用户又不会一打开App就要支付，对其进行懒加载。

4启动页优化

平时我们在开发App时，都会设置一个启动页SplashActivity,然后2或3秒后，并且SplashActivity里面可以去做一些MainActivity的数据的预加载，然后需要通过意图传到MainActivity。 

优点：启动速度有所加快 
缺点：最终还是要进入首页，在进入首页的时候，首页复杂的View渲染以及必须在UI线程执行的业务逻辑，仍然拖慢了启动速度。启动页简单执行快，首页复杂执行慢，前轻后重。

思路：能否在启动页的展示的同时，首页的View就能够被加载，首页的业务逻辑就能够被执行？

优化方向： 

把SplashActivity改成SplashFragment,应用程序的入口仍然是MainActivity,在MainActivity中先展示SplashFragment

当SplashFragment显示完毕后再将它remove，同时在SplashFragment的2S的友好时间内进行网络数据缓存，在窗口加载完毕后，我们加载activity_main的布局，考虑到这个布局有可能比较复杂，耽误View的解析时间，采用ViewStub的形式进行懒加载。

这样一开始只要加载SplashFragment所展示的布局就Ok了。


6.启动优化一些思路


1、避免启动页UI的过度绘制，减少UI重复绘制时间，打开设置中的GPU过度绘制开关，界面整体呈现浅色，特别复杂的界面，红色区域也不应该超过全屏幕的四分之一； 
2、主线程中的所有SharedPreference能否在非UI线程中进行，SharedPreferences的apply函数需要注意，因为Commit函数会阻塞IO，这个函数虽然执行很快，但是系统会有另外一个线程来负责写操作，当apply频率高的时候，该线程就会比较占用CPU资源。类似的还有统计埋点等，在主线程埋点但异步线程提交，频率高的情况也会出现这样的问题。 
3、对于首次启动的黑屏问题，对于“黑屏”是否可以设计一个.9图片替换掉，间接减少用户等待时间。 
4、对于网络错误界面，友好提示界面，使用ViewStub的方式，减少UI一次性绘制的压力。 
5、通过下面这种方式进行懒加载

6、Multidex的使用，也是拖慢启动速度的元凶，必须要做优化
