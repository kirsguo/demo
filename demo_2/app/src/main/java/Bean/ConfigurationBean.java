package Bean;

import java.util.List;

/**
 * Created by Kirsguo on 2017/5/4.
 */

public class ConfigurationBean {

    private List<COMMBean> COMM;
    private List<APPBean> APP;

    public List<COMMBean> getCOMM() {
        return COMM;
    }

    public void setCOMM(List<COMMBean> COMM) {
        this.COMM = COMM;
    }

    public List<APPBean> getAPP() {
        return APP;
    }

    public void setAPP(List<APPBean> APP) {
        this.APP = APP;
    }

    public static class COMMBean {
        /**
         * targetip : 192.168.10.224
         * targetport : 6518
         * myport : 8000
         * appname : V2XAPP
         * boxtype : 1
         */

        private String targetip;
        private String targetport;
        private String myport;
        private String appname;
        private String boxtype;

        public String getTargetip() {
            return targetip;
        }

        public void setTargetip(String targetip) {
            this.targetip = targetip;
        }

        public String getTargetport() {
            return targetport;
        }

        public void setTargetport(String targetport) {
            this.targetport = targetport;
        }

        public String getMyport() {
            return myport;
        }

        public void setMyport(String myport) {
            this.myport = myport;
        }

        public String getAppname() {
            return appname;
        }

        public void setAppname(String appname) {
            this.appname = appname;
        }

        public String getBoxtype() {
            return boxtype;
        }

        public void setBoxtype(String boxtype) {
            this.boxtype = boxtype;
        }
    }

    public static class APPBean {
        /**
         * APPName : TrafficSign
         * enabled : 1
         * ID : V2XAPP_002
         * subscribleMsg : [{"className":"BSMSign","filterCode":"1111010111110001"}]
         * threshold : {"DWTmin":"1","MLT":"1","MDRT":"1","MAT":"1","Epsilon":"1"}
         */

        private String APPName;
        private int enabled;
        private String ID;
        private ThresholdBean threshold;
        private List<SubscribleMsgBean> subscribleMsg;

        public String getAPPName() {
            return APPName;
        }

        public void setAPPName(String APPName) {
            this.APPName = APPName;
        }

        public int getEnabled() {
            return enabled;
        }

        public void setEnabled(int enabled) {
            this.enabled = enabled;
        }

        public String getID() {
            return ID;
        }

        public void setID(String ID) {
            this.ID = ID;
        }

        public ThresholdBean getThreshold() {
            return threshold;
        }

        public void setThreshold(ThresholdBean threshold) {
            this.threshold = threshold;
        }

        public List<SubscribleMsgBean> getSubscribleMsg() {
            return subscribleMsg;
        }

        public void setSubscribleMsg(List<SubscribleMsgBean> subscribleMsg) {
            this.subscribleMsg = subscribleMsg;
        }

        public static class ThresholdBean {
            /**
             * DWTmin : 1
             * MLT : 1
             * MDRT : 1
             * MAT : 1
             * Epsilon : 1
             */

            private String DWTmin;
            private String MLT;
            private String MDRT;
            private String MAT;
            private String Epsilon;

            public String getDWTmin() {
                return DWTmin;
            }

            public void setDWTmin(String DWTmin) {
                this.DWTmin = DWTmin;
            }

            public String getMLT() {
                return MLT;
            }

            public void setMLT(String MLT) {
                this.MLT = MLT;
            }

            public String getMDRT() {
                return MDRT;
            }

            public void setMDRT(String MDRT) {
                this.MDRT = MDRT;
            }

            public String getMAT() {
                return MAT;
            }

            public void setMAT(String MAT) {
                this.MAT = MAT;
            }

            public String getEpsilon() {
                return Epsilon;
            }

            public void setEpsilon(String Epsilon) {
                this.Epsilon = Epsilon;
            }
        }

        public static class SubscribleMsgBean {
            /**
             * className : BSMSign
             * filterCode : 1111010111110001
             */

            private String className;
            private String filterCode;

            public String getClassName() {
                return className;
            }

            public void setClassName(String className) {
                this.className = className;
            }

            public String getFilterCode() {
                return filterCode;
            }

            public void setFilterCode(String filterCode) {
                this.filterCode = filterCode;
            }
        }
    }
}
