## 1.Description 
```groovy
网络检测 服务类
①网络正常时：一分钟执行一次检测
②网络异常时：默认先执行一次检测，然后按周期三分钟执行
软复位：
     如果到达了指定的网络异常次数{@link #nowNetErrCount},方法{@link #resetSoftware4G(boolean)}，执行软复位
硬复位：
     如果到达了指定的软复位执行的次数{@link #max4GResetCount},方法{@link #resetHardWare4G()}，则执行硬复位
注1：网络正常时重置所有状态
注2：网络正常后又异常则执行②
注3：2点整的时候进行软复位一次，时间请设置24小时制

```
## 2. 按如下命令来区分4g模块复位
### 2.1 adb命令详情
现在
----
所有平台 统一命令：“ echo 1 > /dev/lte_state ”

之前
----
 型号  | 对应命令  | 备注
 ---- | ----- | ------  
 rk3399| echo 1 > /sys/class/spi_sim_ctl/state | 7.1.2
 飞思卡尔1| echo 1 > /sys/devices/platform/imx6q_sim/sim_sysfs/state | 4.2.2
 飞思卡尔2| echo 1 > /sys/bus/platform/devices/sim-gpios.40/sim_sysfs/state | 5.1.1

### 2.2 操作步骤
//找到module-main：com.goldze.main.service.NetworkServiceTest 在reSetAndReboot()方法中选择commandToReset对应的命令
```java
 private String[] commandToReset = new String[]{
            "echo 1 > /sys/class/spi_sim_ctl/state",//rk3399
            "echo 1 > /sys/devices/platform/imx6q_sim/sim_sysfs/state"//飞思卡尔
    };
```
```java
 public void reSetAndReboot() {
        Disposable disposable = RxBus.getDefault().toObservable(String.class).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                if (s.equals("reset")) {
                    ....
                    ShellUtils.CommandResult resetCommand = ShellUtils.execCommand(commandToReset[1], false);//在这里设置commandToReset对应的命令
                    .....
                } else {
                }
            }
        });
    }
```
## 3.updateLog
### version 1.0.8~1.0.8
- 去掉原先因为机型的不同而执行的各种命令，现在统一使用一个“ echo 1 > /dev/lte_state ”
- 解决了部分4g模块复位中的计次错误
### version 1.0.7 
- 修改访问地址，机型与对应命令添加到数据库时的判断
- 优化一些代码结构
- 调整代码可读性
### version 1.0.3 ~1.0.6
- 添加适配方案,适应多种分辨率
- version 1.0.3
- 添加中英文支持
- 动态添加/删除访问的访问地址  系统内置的除外
- 动态添加/删除机型  系统内置的除外
### version 1.0.2
- 设置开机启动时不显示Activity，只加载后台检测网络服务
### version 1.0.1
- 修改并优化部分代码，周期和判定次数的设置重启
### version 1.0
- 第一次提交
## License

    Copyright 2018 chtj
 
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
 
        http://www.apache.org/licenses/LICENSE-2.0
 
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.