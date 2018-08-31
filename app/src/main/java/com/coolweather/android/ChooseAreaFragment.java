package com.coolweather.android;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {

    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;

    private TextView titleText;

    private Button backButton;

    private ListView listView;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();

    /*
    * 省列表
    * */
    private List<Province> provincesList;

    /*
    * 市列表
    * */
    private List<City> cityList;

    /*
    * 县列表
    * */
    private List<County> countyList;

    /*
    * 选中的省份
    * */
    private Province selectedProvince;

    /*
    * 选中的城市
    * */
    private City selectedCity;

    /*
    * 当前选中的级别
    * */
    private int currentLevel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {     //重写 Fragment 的 onCreateView  。 碎片的生命周期函数 加载布局时调用
        View view = inflater.inflate(R.layout.choose_area, container, false);               //然后用LayoutInflater 的 inflate() 方法将 choose_area 布局动态的加载进来
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);      //借助 ArrayAdapter 适配器创建好适配器对象
        listView.setAdapter(adapter);                                                                       //调用 ListView 的 setAdapter() 方法 将构建好的适配器对象传递进去，这样 listView 和数据的关联就建立完成了
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {                      // 碎片的生命周期函数 关联的活动创建完毕时调用
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {      //创建 列表的点击事件
                if (currentLevel == LEVEL_PROVINCE) {       // 判断当前页面显示数据是不是 省级列表
                    selectedProvince = provincesList.get(position);     //获取点击的省的数据
                    queryCities();                                          //查询当前省下所有的市
                } else if (currentLevel == LEVEL_CITY) {    //判断当前页面显示数据是不是 市级列表
                    selectedCity = cityList.get(position);     //获取点击的市的数据
                    queryCounties();                              //查询当前市下所有的 县级列表
                } else if (currentLevel == LEVEL_COUNTY) {
                    String weatherId = countyList.get(position).getWeatherId();
                    if (getActivity() instanceof MainActivity) {
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    } else if (getActivity() instanceof WeatherActivity) {
                        WeatherActivity activity = (WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefresh.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {                //创建 按钮的点击事件
                if (currentLevel == LEVEL_COUNTY) {             //判断当前页面显示数据是不是 县级列表
                    queryCities();                                  //点击返回按钮时 获取市级列表数据
                } else if (currentLevel == LEVEL_CITY) {       //判断当前页面显示数据是不是 市级列表
                    queryProvinces();                               //点击返回按钮时 获取省级列表
                }
            }
        });
        queryProvinces();       // 指定一开始就加载此方法 获取所有的省级数据
    }

    /*
     * 查询全国所有的省， 优先从数据库查询， 如果没有查询到再到服务器上去查询
     * */
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);        //当当前列表显示的是省级数据时，隐藏返回按钮
        provincesList = LitePal.findAll(Province.class);        //查询 省级数据库中所有的数据
        if (provincesList.size() > 0) {             //有数据时
            dataList.clear();               //清空 dataList 数组
            for (Province province : provincesList) {
                dataList.add(province.getProvinceName());       //遍历所有省的名字 存入dataList
            }
            adapter.notifyDataSetChanged();     //通知布局 数据发生了变化
            listView.setSelection(0);           //列表滚动到最顶部
            currentLevel = LEVEL_PROVINCE;
        } else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");           //通过上面的地址 从服务器获取数据
        }
    }

    /*
    * 查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器查询
    * */
    private void queryCities(){
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);                     //显示返回按钮
        cityList = LitePal.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        }else{
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    /*
     * 查询选中市内所有的县， 优先从数据库中查询，如果没有查询到再去服务器查询
     * */
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = LitePal.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" +cityCode;
            queryFromServer(address, "county");
        }
    }

    /*
    * 根据传入的地址和类型从服务器上查询省市县数据
    * */

    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                }
                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                //通过 runOnUiRhread() 方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        });
    }

    /*
    * 显示进度对话框
    * */
    private void showProgressDialog(){
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /*
    * 关闭进度对话框
    * */
    private void closeProgressDialog(){
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}
