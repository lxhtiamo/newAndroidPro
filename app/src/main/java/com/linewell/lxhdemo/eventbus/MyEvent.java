package com.linewell.lxhdemo.eventbus;
/*
使用方法:
发送一般的事件,参数1:标识,参数2,数据.
EventBus.getDefault().post(new MyEvent(MyEvent.What.example, 21111));
发送黏性事件
EventBus.getDefault().postSticky(new MyEvent(MyEvent.What.example, 21111));
注:黏性事件是指接收的view还没有初始化,比如,我要A->B  B还未加载的时候用黏性事件
接收一般事件
@Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveEvent(MyEvent event) {
        switch (event.what) {
            case MyEvent.What.example:
                Object data = event.data; //可以强转为任何结构数据
                doing things...........
                break;
         }
     }

接收黏性事件
@Subscribe(threadMode = ThreadMode.MAIN,sticky = true)
* */

public class MyEvent {

    public int what;
    public Object data;

    public MyEvent(int what, Object data) {
        this.what = what;
        this.data = data;
    }

    //标识.......
    public interface What {
        int example = 1;//例子
    }
}
