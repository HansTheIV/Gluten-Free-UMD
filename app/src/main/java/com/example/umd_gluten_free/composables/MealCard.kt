package com.example.umd_gluten_free.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.umd_gluten_free.data.Meal
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Composable
fun MealCard(
    meal: Meal
) {
    Card(
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .padding(
                start = 8.dp,
                end = 8.dp,
                top = 4.dp,
                bottom = 4.dp
            )
            .fillMaxWidth(),
        elevation = 3.dp,
        border = BorderStroke(0.5.dp, color = Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 12.dp)
        ){
            meal.name?.let { name ->
                Text(
                    text = name + "\n\uD83C\uDF1F" + meal.rating.toString(),
                    modifier = Modifier
                        .fillMaxWidth(0.80f)
                        .wrapContentWidth(Alignment.Start),
                    color = Color.DarkGray,
                    fontSize = 20.sp
                )
            }
            meal.location?.let { location ->
                Text(
                    text = location.replace(" ", "\n"),
                    color = Color.DarkGray,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Justify
                )
            }
        }
    }
}