# LogRecord


LogRecord.get().init(this).setFolder("dmr")
.setInformat("yyyy-MM-dd")
.setNotifyDesc("日志记录服务")
.setNotifyId(12)
.setNotifyIcon(R.mipmap.ic_launcher)
.setOffDesc("关闭日志")
.setOnDesc("开始日志")
.setControlBroadcastAction("dmr_log_start","dmr_log_stop")
.setFolder("ALOG")
.setPrefix("DMR")