## 1.Description 
```groovy
//该项目主要是定期检查4G网络是否正常连接，以下为状态的操作流程
//正常状态：此时虽然能够连接4G网络，但是避免中途产生断网情况，需要按设定的周期检测网络情况
//异常状态：产生网络异常的情况下，需要对断网情况周期缩短至2分钟检测一次，并且按设定的次数进行判定，达到次数时按 step2步骤来操作
```
## 2.平台节点不一致 按如下命令来区分4g模块复位
### 2.1 adb命令详情
 型号  | 对应命令  | 备注
 ---- | ----- | ------  
 rk3399  | echo 1 > /sys/class/spi_sim_ctl/state | commandToReset[0] 
 飞思卡尔  | echo 1 > /sys/devices/platform/imx6q_sim/sim_sysfs/state | commandToReset[1]  

### 2.2 操作步骤
//找到module-main：com.goldze.main.service.NetWorkService 在reSetAndReboot()方法中选择commandToReset对应的命令
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

### version 1.0.3 ~1.0.6
-添加适配方案,适应多种分辨率
### version 1.0.3

-添加中英文支持

-动态添加/删除访问的访问地址  系统内置的除外

-动态添加/删除机型  系统内置的除外
### version 1.0.2
-设置开机启动时不显示Activity，只加载后台检测网络服务
### version 1.0.1
-修改并优化部分代码，周期和判定次数的设置重启
### version 1.0
-第一次提交
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