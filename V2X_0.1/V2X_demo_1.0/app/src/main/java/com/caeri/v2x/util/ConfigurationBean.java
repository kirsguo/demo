package com.caeri.v2x.util;

import android.content.Context;

import com.google.gson.GsonBuilder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by JeRome on 2017/4/17.
 */

public class ConfigurationBean {


    private List<COMMBean> COMM;
    private ArrayList<APPBean> APP;

    public List<COMMBean> getCOMM() {
        return COMM;
    }

    public void setCOMM(List<COMMBean> COMM) {
        this.COMM = COMM;
    }

    public ArrayList<APPBean> getAPP() {
        return APP;
    }

    public void setAPP(ArrayList<APPBean> APP) {
        this.APP = APP;
    }

    public static class COMMBean {
        /**
         * targetip : 192.168.10.224
         * targetport : 6518
         * myport : 9999
         * appname : V2XAPP
         * boxtype : 1
         */

        private String targetip;
        private String targetport;
        private String myport;
        private String appname;
        private String boxtype;

        public static void loadConfigToCOMM(Context con, String confName, StringBuffer targetip,
                                            StringBuffer targetport, StringBuffer myport, StringBuffer appname, StringBuffer boxtype) throws Exception{
            ConfigurationBean jsonBean = new ConfigurationBean();
            InputStream inputStream = null;
            try {
                inputStream = con.getAssets().open(confName);


                jsonBean = new GsonBuilder().create().fromJson(new InputStreamReader(inputStream), ConfigurationBean.class);

                if (jsonBean.getCOMM().get(0).getTargetip() != null && jsonBean.getCOMM().get(0).getTargetip() != "") {
                    targetip.delete(0, targetip.length());
                    targetip.append(jsonBean.getCOMM().get(0).getTargetip());
                }
                if (jsonBean.getCOMM().get(0).getTargetport() != null && jsonBean.getCOMM().get(0).getTargetport() != "") {
                    targetport.delete(0, targetport.length());
                    targetport.append(jsonBean.getCOMM().get(0).getTargetport());
                }
                if (jsonBean.getCOMM().get(0).getMyport() != null && jsonBean.getCOMM().get(0).getMyport() != "") {
                    myport.delete(0, myport.length());
                    myport.append(jsonBean.getCOMM().get(0).getMyport());
                }
                if (jsonBean.getCOMM().get(0).getAppname() != null && jsonBean.getCOMM().get(0).getAppname() != "") {
                    appname.delete(0, appname.length());
                    appname.append(jsonBean.getCOMM().get(0).getAppname());
                }
                if (jsonBean.getCOMM().get(0).getBoxtype() != null && jsonBean.getCOMM().get(0).getBoxtype() != "") {
                    boxtype.delete(0, boxtype.length());
                    boxtype.append(jsonBean.getCOMM().get(0).getBoxtype());
                }
            }
            catch (Exception e) {
                Exception se= new Exception(Constants.COMM_EXCEPTION_READ_FILE);
                throw se;
                //e.printStackTrace();
            }
        }

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
         * APPName : APPMe
         * ID : V2XAPP_001
         * type : 0
         * enabled : 1
         * subscribleMsg : [{"className":"","filterCode":""}]
         * threshold : {"arg1":"","arg2":"","arg3":"","arg4":"","arg5":""}
         */

        private String APPName;
        private String ID;
        private int type;
        private int enabled;
        private ThresholdBean threshold;
        private ArrayList<SubscribleMsgBean> subscribleMsg;

        public String getAPPName() {
            return APPName;
        }

        public void setAPPName(String APPName) {
            this.APPName = APPName;
        }

        public String getID() {
            return ID;
        }

        public void setID(String ID) {
            this.ID = ID;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public int getEnabled() {
            return enabled;
        }

        public void setEnabled(int enabled) {
            this.enabled = enabled;
        }

        public ThresholdBean getThreshold() {
            return threshold;
        }

        public void setThreshold(ThresholdBean threshold) {
            this.threshold = threshold;
        }

        public ArrayList<SubscribleMsgBean> getSubscribleMsg() {
            return subscribleMsg;
        }

        public void setSubscribleMsg(ArrayList<SubscribleMsgBean> subscribleMsg) {
            this.subscribleMsg = subscribleMsg;
        }

        public static class ThresholdBean {
            /**
             * arg1 :
             * arg2 :
             * arg3 :
             * arg4 :
             * arg5 :
             */

            private String arg1;
            private String arg2;
            private String arg3;
            private String arg4;
            private String arg5;

            public String getArg1() {
                return arg1;
            }

            public void setArg1(String arg1) {
                this.arg1 = arg1;
            }

            public String getArg2() {
                return arg2;
            }

            public void setArg2(String arg2) {
                this.arg2 = arg2;
            }

            public String getArg3() {
                return arg3;
            }

            public void setArg3(String arg3) {
                this.arg3 = arg3;
            }

            public String getArg4() {
                return arg4;
            }

            public void setArg4(String arg4) {
                this.arg4 = arg4;
            }

            public String getArg5() {
                return arg5;
            }

            public void setArg5(String arg5) {
                this.arg5 = arg5;
            }
        }

        public static class SubscribleMsgBean {
            /**
             * className :
             * filterCode :
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
