package Bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Kirsguo on 2017/4/28.
 */

public class JsonBean {

    @SerializedName("长春市中学")
    private List<schoolList> schools;

    public List<schoolList> getSchools() {
        return schools;
    }

    public void setSchools(List<schoolList> schools) {
        this.schools = schools;
    }

    public static class schoolList {
        /**
         * 学校名称 : 吉大附中
         * ID : sch_001
         * 学校部门 : [{"部门名称":"财务部","部门标号":"bm_001"},{"部门名称":"事业部","部门标号":"bm_002"}]
         */

        @SerializedName("学校名称")
        private String schoolName;
        @SerializedName("ID")
        private String schoolId;
        @SerializedName("学校部门")
        private List<schoolPartList> schoolPart;

        public String getSchoolName() {
            return schoolName;
        }

        public void setSchoolName(String schoolName) {
            this.schoolName = schoolName;
        }

        public String getSchoolId() {
            return schoolId;
        }

        public void setSchoolId(String schoolId) {
            this.schoolId = schoolId;
        }

        public List<schoolPartList> getSchoolPart() {
            return schoolPart;
        }

        public void setSchoolPart(List<schoolPartList> schoolPart) {
            this.schoolPart = schoolPart;
        }

        public static class schoolPartList {
            /**
             * 部门名称 : 财务部
             * 部门标号 : bm_001
             */

            @SerializedName("部门名称")
            private String partNamr;
            @SerializedName("部门标号")
            private String partId;

            public String getPartNamr() {
                return partNamr;
            }

            public void setPartNamr(String partNamr) {
                this.partNamr = partNamr;
            }

            public String getPartId() {
                return partId;
            }

            public void setPartId(String partId) {
                this.partId = partId;
            }
        }
    }
}
