package vn.edu.usth.myapplication;

import java.util.HashMap;
import java.util.Map;

public class VocabMap {

    public static final Map<String, String> EN_TO_VI = new HashMap<>();

    static {
        EN_TO_VI.put("person","người"); EN_TO_VI.put("bicycle","xe đạp");
        EN_TO_VI.put("car","ô tô"); EN_TO_VI.put("motorcycle","xe máy");
        EN_TO_VI.put("airplane","máy bay"); EN_TO_VI.put("bus","xe buýt");
        EN_TO_VI.put("train","tàu hỏa"); EN_TO_VI.put("truck","xe tải");
        EN_TO_VI.put("boat","thuyền"); EN_TO_VI.put("traffic light","đèn giao thông");
        EN_TO_VI.put("fire hydrant","vòi cứu hỏa"); EN_TO_VI.put("stop sign","biển dừng");
        EN_TO_VI.put("bench","ghế băng"); EN_TO_VI.put("bird","con chim");
        EN_TO_VI.put("cat","con mèo"); EN_TO_VI.put("dog","con chó");
        EN_TO_VI.put("horse","con ngựa"); EN_TO_VI.put("cow","con bò");
        EN_TO_VI.put("elephant","con voi"); EN_TO_VI.put("bear","con gấu");
        EN_TO_VI.put("zebra","ngựa vằn"); EN_TO_VI.put("giraffe","hươu cao cổ");
        EN_TO_VI.put("backpack","balo"); EN_TO_VI.put("umbrella","ô dù");
        EN_TO_VI.put("handbag","túi xách"); EN_TO_VI.put("suitcase","vali");
        EN_TO_VI.put("bottle","chai nước"); EN_TO_VI.put("wine glass","ly rượu");
        EN_TO_VI.put("cup","cái cốc"); EN_TO_VI.put("fork","cái nĩa");
        EN_TO_VI.put("knife","con dao"); EN_TO_VI.put("spoon","cái thìa");
        EN_TO_VI.put("bowl","cái bát"); EN_TO_VI.put("banana","quả chuối");
        EN_TO_VI.put("apple","quả táo"); EN_TO_VI.put("sandwich","bánh sandwich");
        EN_TO_VI.put("orange","quả cam"); EN_TO_VI.put("broccoli","bông cải xanh");
        EN_TO_VI.put("carrot","cà rốt"); EN_TO_VI.put("hot dog","xúc xích");
        EN_TO_VI.put("pizza","bánh pizza"); EN_TO_VI.put("donut","bánh donut");
        EN_TO_VI.put("cake","bánh ngọt"); EN_TO_VI.put("chair","cái ghế");
        EN_TO_VI.put("couch","ghế sofa"); EN_TO_VI.put("potted plant","cây cảnh");
        EN_TO_VI.put("bed","cái giường"); EN_TO_VI.put("dining table","bàn ăn");
        EN_TO_VI.put("toilet","nhà vệ sinh"); EN_TO_VI.put("tv","tivi");
        EN_TO_VI.put("laptop","máy tính xách tay"); EN_TO_VI.put("mouse","chuột máy tính");
        EN_TO_VI.put("remote","điều khiển từ xa"); EN_TO_VI.put("keyboard","bàn phím");
        EN_TO_VI.put("cell phone","điện thoại"); EN_TO_VI.put("microwave","lò vi sóng");
        EN_TO_VI.put("oven","lò nướng"); EN_TO_VI.put("toaster","máy nướng bánh");
        EN_TO_VI.put("sink","bồn rửa"); EN_TO_VI.put("refrigerator","tủ lạnh");
        EN_TO_VI.put("book","quyển sách"); EN_TO_VI.put("clock","đồng hồ");
        EN_TO_VI.put("vase","bình hoa"); EN_TO_VI.put("scissors","cái kéo");
        EN_TO_VI.put("teddy bear","gấu bông"); EN_TO_VI.put("hair drier","máy sấy tóc");
        EN_TO_VI.put("toothbrush","bàn chải đánh răng"); EN_TO_VI.put("sheep","con cừu");
    }

    public static String getVI(String labelEn) {
        String vi = EN_TO_VI.get(labelEn.toLowerCase());
        return vi != null ? vi : labelEn;
    }
}