package pl.nowicki.openweatherdexprotector

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pl.nowicki.openweatherdexprotector.model.WeatherRsp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    val SERVER_URL = "https://api.openweathermap.org"

    lateinit var viewModel: WeatherViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val weatherApiService = getForecastService(this);

        val cities: MutableList<String> = mutableListOf("Katowice", "London", "Ulcinj")
        var cityCounter = 0

        button.setOnClickListener {
            val city = cities.get(++cityCounter % 3)

            val weatherCall = getForecast(weatherApiService, city)
            weatherCall.enqueue(object : Callback<WeatherRsp> {
                override fun onFailure(call: Call<WeatherRsp>?, t: Throwable?) {
                    tv.text = "FAIL"
                }

                override fun onResponse(call: Call<WeatherRsp>?, response: Response<WeatherRsp>?) {
                    val sb = StringBuilder(response?.body()?.city!!.name.toString())
                    sb.append(" ")
                    sb.append(response.body()?.list!![0].main.temp.toString())
                    tv.text = sb.toString()
                }
            })
        }
    }

    fun initModel() {
        viewModel = ViewModelProviders.of(this).get(WeatherViewModel::class.java)
    }

    private fun getForecastService(context: Context): WeatherApiService {

        val cookieManager = CookieManager()
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)

        val okBuilder = OkHttpClient.Builder()
        okBuilder.cookieJar(JavaNetCookieJar(cookieManager))

        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        okBuilder.addNetworkInterceptor(logging)
        okBuilder.readTimeout(context.resources.getInteger(R.integer.read_timeout).toLong(), TimeUnit.MILLISECONDS)
        okBuilder.connectTimeout(context.resources.getInteger(R.integer.connect_timeout).toLong(), TimeUnit.MILLISECONDS)

        val retrofit = Retrofit.Builder()
                .baseUrl(SERVER_URL)
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
                .client(okBuilder.build())
                .build()

        return retrofit.create(WeatherApiService::class.java)
    }

    private fun getForecast(weatherService: WeatherApiService, city: String): Call<WeatherRsp> {
        return weatherService.forecast(city)
    }

}
