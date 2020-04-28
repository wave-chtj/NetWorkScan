## 1.Description 
- library-base 基础工具类
- library-res  基础UI库
- lte_network  4G硬复位
- lte_network-Reset  4G硬复位,软复位
- eth-network-enable 以太网启用



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