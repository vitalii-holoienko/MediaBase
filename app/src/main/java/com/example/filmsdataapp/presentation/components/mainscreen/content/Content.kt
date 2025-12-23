package com.example.filmsdataapp.presentation.components.mainscreen.content

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row


import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.filmsdataapp.R
import com.example.filmsdataapp.domain.model.News
import com.example.filmsdataapp.domain.model.Title
import com.example.filmsdataapp.presentation.viewmodels.MainActivityViewModel
import com.example.filmsdataapp.ui.theme.BackGroundColor
import com.example.filmsdataapp.ui.theme.PrimaryColor
import com.example.filmsdataapp.ui.theme.TextColor
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Composable
fun Content(
    viewModel : MainActivityViewModel
) {
    val comingSoonMovies = viewModel.comingSoonMovies.observeAsState(emptyList())
    val currentlyTrendingMovies = viewModel.currentlyTrendingMovies.observeAsState(emptyList())
    Log.d("TEKKEN", "!!!!! " + currentlyTrendingMovies.value.size.toString())
    val news = viewModel.news.observeAsState(emptyList())
    val isConnected by viewModel.isConnected.collectAsState()

    LaunchedEffect(currentlyTrendingMovies) {
        Log.d("TEKKEN", "Currently trending movies changed, size = ${currentlyTrendingMovies.value.size}")
    }


    Box(modifier = Modifier
        .fillMaxSize()
        .padding(10.dp)
    ){
        Column(){
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(35.dp)
                .background(color = PrimaryColor)
                .clickable {
                    if(isConnected){
                        viewModel.onCurrentlyTrendingTitlesClicked()
                    }

                }
            ){
                Text(
                    text = "Currently trending",
                    color = TextColor,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(5.dp, 0.dp)
                )

                Image(
                    painter = painterResource(id = R.drawable.triangle_go_to_icon),
                    colorFilter = ColorFilter.tint(Color.White),
                    contentDescription = "",
                    modifier = Modifier
                        .size(25.dp)
                        .align(Alignment.CenterEnd)
                        .scale(1f)
                        .padding(5.dp, 0.dp),

                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            Box(modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(color = PrimaryColor),
                contentAlignment = Alignment.Center
            ){
                ImageSlider(currentlyTrendingMovies.value!!, viewModel)
            }
            Spacer(modifier = Modifier.height(70.dp))
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(35.dp)
                .background(color = PrimaryColor)
                .clickable {
                    if(isConnected){
                        viewModel.onComingSoonTitlesClicked()
                    }

                }
            ){
                Text(
                    text = "Coming soon",
                    color = TextColor,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(5.dp, 0.dp)
                )
                Image(
                    painter = painterResource(id = R.drawable.triangle_go_to_icon),
                    contentDescription = "",
                    Modifier
                        .size(25.dp)
                        .align(Alignment.CenterEnd)
                        .scale(1f)
                        .padding(5.dp, 0.dp)

                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryColor)
                .height(220.dp),
                contentAlignment = Alignment.Center
            ){
                ImageSlider(comingSoonMovies.value!!, viewModel)
            }
            Spacer(modifier = Modifier.height(40.dp))
            //MAIN TAGS
            Row(modifier = Modifier
                .fillMaxWidth()
                .height(35.dp)){
                Box(modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(color = Color.White).clickable {
                        if(isConnected){
                            viewModel.onMoviesClicked()
                        }

                    }
                ){
                    Row(modifier = Modifier
                        .fillMaxSize()
                        .background(color = Color(254, 194, 197))){
                        Box(modifier = Modifier
                            .fillMaxHeight()
                            .width(8.dp)
                            .background(color = Color(255, 159, 140)).clickable {
                                if(isConnected){
                                    viewModel.onMoviesClicked()
                                }
                            })
                        Text(
                            text = "Movies",
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(5.dp, 0.dp, 0.dp, 0.dp)
                                .weight(1f),
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.notosans_variablefont_wdth_wght)),
                            color = Color(252,87,94)
                        )
                        Image(
                            painter = painterResource(id = R.drawable.triangle_go_to_icon),
                            contentDescription = "",
                            Modifier
                                .size(25.dp)
                                .scale(1f)
                                .align(Alignment.CenterVertically)
                                .padding(5.dp, 0.dp)

                        )

                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(color = Color.White)
                    .clickable {
                        if(isConnected){
                            viewModel.onTVShowsClicked()
                        }

                    }
                ){
                    Row(modifier = Modifier
                        .fillMaxSize()
                        .background(color = Color(218, 241, 255))){
                        Box(modifier = Modifier
                            .fillMaxHeight()
                            .width(8.dp)
                            .background(color = Color(188, 230, 255)))
                        Text(
                            text = "TV Shows",
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(5.dp, 0.dp, 0.dp, 0.dp)
                                .weight(1f),
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.notosans_variablefont_wdth_wght)),
                            color = Color(68,187,255)
                        )
                        Image(
                            painter = painterResource(id = R.drawable.triangle_go_to_icon),
                            contentDescription = "",
                            Modifier
                                .size(25.dp)
                                .scale(1f)
                                .align(Alignment.CenterVertically)
                                .padding(5.dp, 0.dp)

                        )

                    }
                }
                Spacer(modifier = Modifier.width(10.dp))

                Box(modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(color = Color.White)){
                    Row(modifier = Modifier
                        .fillMaxSize()
                        .background(color = Color(231, 246, 218))
                        .clickable {
                            if(isConnected){
                                viewModel.onActorsClicked()
                            }

                        }
                    ){
                        Box(modifier = Modifier
                            .fillMaxHeight()
                            .width(8.dp)
                            .background(color = Color(215, 239, 195)))
                        Text(
                            text = "Actors",
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(5.dp, 0.dp, 0.dp, 0.dp)
                                .weight(1f),
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.notosans_variablefont_wdth_wght)),
                            color = Color(116,214,31)
                        )
                        Image(
                            painter = painterResource(id = R.drawable.triangle_go_to_icon),
                            contentDescription = "",
                            Modifier
                                .size(25.dp)
                                .scale(1f)
                                .align(Alignment.CenterVertically)
                                .padding(5.dp, 0.dp)

                        )

                    }
                }
            }
            Spacer(modifier = Modifier.height(15.dp))
            //NEWS
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(35.dp)
                .background(color = Color.White)){
                Row(modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color(255, 232, 216))){
                    Box(modifier = Modifier
                        .fillMaxHeight()
                        .width(8.dp)
                        .background(color = Color(255, 213, 184)))
                    Text(
                        text = "NEWS",
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(5.dp, 0.dp, 0.dp, 0.dp)
                            .weight(1f),
                        fontSize = 18.sp,
                        fontFamily = FontFamily(Font(R.font.notosans_variablefont_wdth_wght)),
                        color = Color(255, 191, 95)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            News(news.value, viewModel)
        }

    }
}
