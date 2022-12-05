package com.example.umd_gluten_free


import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.umd_gluten_free.composables.MealCard
import com.example.umd_gluten_free.data.Meal
import com.example.umd_gluten_free.ui.theme.UMDGlutenFreeTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await


class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db : DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        db = Firebase.database.reference

        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {

            UMDGlutenFreeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    AppNavHost(context = LocalContext.current, auth = auth, db = db)

                }
            }
        }
    }

}

@Composable
fun AppNavHost(
    auth: FirebaseAuth,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = "homeScreen",
    context: Context,
    db: DatabaseReference
) {
    val filter = remember { mutableStateOf(false)}
    val filterGreaterThan = remember { mutableStateOf(0f)}
    NavHost(modifier=modifier, navController=navController, startDestination=startDestination) {
        composable("accountManagement") {
            if (auth.currentUser != null) {
                // If the user is currently logged in
                LogoutScreen(auth = auth, onNavigateToMap = {navController.navigate("homeScreen")}, context = LocalContext.current)
            } else {
                LoginScreen(
                    onNavigateToForgotPass = {navController.navigate("forgotPassword")},
                    onNavigateToSignup = {navController.navigate("signupScreen")},
                    auth = auth,
                    context = context,
                    onNavigateHome = { navController.navigate("homeScreen") }
                )
            }
        }
        composable("listScreen") { ListScreen(
            db = db,
            toastContext = context,
            filter = filter,
            filterGreaterThan = filterGreaterThan
        ) }
        composable("submitNewFood") {
            if(auth.currentUser != null) {
                SubmitScreen(
                    context = context,
                    db = db
                ) { navController.navigate("homeScreen") }
            } else {
                Toast.makeText(context, "You cannot submit unless you are logged in.", Toast.LENGTH_SHORT).show()
                LoginScreen(
                    onNavigateToForgotPass = {navController.navigate("forgotPassword")},
                    onNavigateToSignup = {navController.navigate("signupScreen")},
                    auth = auth,
                    context = context,
                    onNavigateHome = { navController.navigate("homeScreen") }
                    )
            }
        }
        composable("forgotPassword") {ForgotPasswordScreen(auth = auth, context = context)}
        composable("signupScreen") {SignupScreen(
            auth = auth,
            context = context,
            onNavigateToMap = { navController.navigate("homeScreen") })}
        composable("loginScreen") {
            LoginScreen(
                onNavigateToForgotPass = {navController.navigate("forgotPassword")},
                onNavigateToSignup = {navController.navigate("signupScreen")},
                auth = auth,
                context = context,
                onNavigateHome = { navController.navigate("homeScreen") },
            )
        }
        composable("homeScreen") {
            homeScreen(
                onNavigateToAcctManagement = { navController.navigate("accountManagement") },
                onNavigateToSubmit = { navController.navigate("submitNewFood") },
                auth = auth,
                db = db,
                toastContext = context,
                filter = filter,
                filterGreaterThan = filterGreaterThan
            )
        }


    }
}
// Top level screens
@Composable

fun SubmitScreen(
    context: Context,
    db: DatabaseReference,
    onNavigateHome: () -> Unit,
) {

    fun submitMeal(mealName:String, locationName: String, rating: Int) {

        val key = db.child("meals").push().key
                if (key == null) {
                    Toast.makeText(context, "Error submitting meal to server", Toast.LENGTH_SHORT).show()
                    return
                }
        val meal = hashMapOf(
            "mealName" to mealName,
            "locationName" to locationName,
            "rating" to rating
        )

        val childUpdates = hashMapOf<String, Any>(
            "meals/$key" to meal
        )
        db.updateChildren(childUpdates)
    }
    Column(modifier = Modifier.fillMaxSize()) {
        val centered = Modifier.align(Alignment.CenterHorizontally)
        val mealName = remember { mutableStateOf(TextFieldValue()) }
        val mealLocation = remember { mutableStateOf(TextFieldValue()) }
        val rating = remember { mutableStateOf(0f) }
        Spacer(modifier = Modifier.height(80.dp))
        TextField(
            label = { Text(text = "What did you eat?") },
            value = mealName.value,
            onValueChange = { mealName.value = it },
            modifier = centered
        )

        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            label = { Text("Where did you eat it?") },
            value = mealLocation.value,
            onValueChange = { mealLocation.value = it },
            modifier = centered
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(text="What would you rate it? (0-5)", modifier = Modifier.align(Alignment.CenterHorizontally))
        Slider(
            value = rating.value,
            onValueChange = {rating.value = it},
            valueRange = 0f..5f,
            steps = 4,
            colors = SliderDefaults.colors(
                thumbColor = Color.Red,
                activeTrackColor = Color.Red
            ),
            modifier = Modifier.padding(60.dp, 10.dp)
        )
        Spacer(modifier = Modifier.height(30.dp))
        Button(
            onClick = {
                runBlocking {
                    submitMeal(
                        mealName.value.text,
                        mealLocation.value.text,
                        rating.value.toInt()
                    )
                }
            },
            modifier = centered,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFe21833))
        ) {
            Text("Submit Meal", color = Color.White)
        }
    }

}

@Composable
fun ListScreen(
    db: DatabaseReference,
    toastContext: Context,
    filter: MutableState<Boolean>,
    filterGreaterThan: MutableState<Float>
) {
    val mealList = ArrayList<Meal>()

    suspend fun getProductsFromFirestore() {
        val mealIds = db.child("Meals")
                        .get()
                        .await()
                        .getChildren()
        try {
            for (currentMeal in mealIds) {
                val newMeal: Meal = currentMeal.getValue(Meal::class.java)!!
                mealList.add(newMeal)
                Log.e("NEW MEAL ADDED", mealList.toString())
            }
        }
        catch (E: java.lang.Exception) {
            Toast.makeText(toastContext, "Error in retrieving data from server", Toast.LENGTH_LONG).show()
        }

    }
    runBlocking { getProductsFromFirestore() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray),
    ){
        LazyColumn {
            item {
                Card(shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .padding(
                            start = 0.dp,
                            end = 0.dp,
                            top = 2.dp,
                            bottom = 4.dp
                        )
                        .fillMaxWidth()
                        .height(40.dp),
                    elevation = 3.dp,
                    border = BorderStroke(0.75.dp, color = Color.Black),
                    backgroundColor = Color.LightGray
                ) {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 12.dp)) {
                        Text(
                            text = "Food / Rating",
                            modifier = Modifier
                                .fillMaxWidth(0.80f)
                                .wrapContentWidth(Alignment.Start),
                            color = Color.DarkGray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Location",
                            color = Color.DarkGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Justify
                        )
                    }
                }
            }
            items(
                items = mealList
            ) { meal ->
                MealCard(meal = meal)
            }
        }
    }
}

@Composable
fun SignupScreen(auth: FirebaseAuth,
                 context: Context,
                 onNavigateToMap: () -> Unit) {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val email = remember { mutableStateOf(TextFieldValue()) }
        val password = remember { mutableStateOf(TextFieldValue()) }
        Text(text = "Sign Up", style = TextStyle(fontSize = 40.sp))

        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            label = { Text(text = "Email Address") },
            value = email.value,
            onValueChange = { email.value = it })

        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            label = { Text(text = "Password (6 characters or more)") },
            value = password.value,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            onValueChange = { password.value = it })
        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier.padding(40.dp, 0.dp, 40.dp, 0.dp)) {
            Button(
                onClick = {
                          auth.createUserWithEmailAndPassword(
                              email.value.text.trim(),
                              password.value.text.trim()
                          ).addOnCompleteListener {
                              if (it.isSuccessful) {
                                  Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                                  onNavigateToMap()
                              } else {
                                  Toast.makeText(context, it.exception?.message, Toast.LENGTH_LONG).show()
                              }
                          }
                },
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFe21833))
            ) {
                Text(text = "Sign Up", color = Color.White)
            }
        }
    }
}

@Composable

fun ForgotPasswordScreen(auth: FirebaseAuth, context: Context) {
    val email = remember { mutableStateOf(TextFieldValue()) }
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(15.dp))
        Text(text = "Reset your password: ", modifier = Modifier.align(Alignment.CenterHorizontally), fontSize = 21.sp)
        Spacer(modifier = Modifier.height(40.dp))
        TextField(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            value = email.value,
            onValueChange = {email.value = it},
            label = {Text("Email Address")})
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = {
                auth.sendPasswordResetEmail(email.value.text.trim())
                Toast.makeText(context, "Password reset email sent!", Toast.LENGTH_SHORT).show()
                      },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFe21833))
        ) {
            Text(text = "Send Reset Email", color = Color.White)
        }
    }
}

//sub-screens
@Composable
fun LoginScreen(
    onNavigateToForgotPass: () -> Unit,
    onNavigateToSignup: () -> Unit,
    auth: FirebaseAuth,
    onNavigateHome: () -> Unit,
    context: Context

    ) {
    Box(modifier = Modifier.fillMaxSize()) {
        Button(onClick = onNavigateToSignup,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
            modifier = Modifier.align(Alignment.BottomCenter)) {
            Text("Sign Up")
        }
    }
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val email = remember { mutableStateOf(TextFieldValue()) }
        val password = remember { mutableStateOf(TextFieldValue()) }

        Text(text = "Login or Sign Up", style = TextStyle(fontSize = 40.sp))

        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            label = { Text(text = "Email Address") },
            value = email.value,
            onValueChange = { email.value = it })

        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            label = { Text(text = "Password") },
            value = password.value,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            onValueChange = { password.value = it })

        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier.padding(40.dp, 0.dp, 40.dp, 0.dp)) {
            Button(
                onClick = {
                    auth.signInWithEmailAndPassword(
                        email.value.text.trim(),
                        password.value.text.trim()
                    ).addOnCompleteListener {
                        if(it.isSuccessful) {
                            //redirect to map, login-successful toast message
                            onNavigateHome()
                            Toast.makeText(context, "Login successful!", Toast.LENGTH_LONG).show()
                        }
                        else {
                            // toast failure message
                            Toast.makeText(context, "Error: " + it.exception?.message, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFe21833))
            ) {
                Text(text = "Login", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onNavigateToForgotPass,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)) {
            Text("Forgot Password?")
        }
    }
}

@Composable
fun LogoutScreen(onNavigateToMap: () -> Unit, auth: FirebaseAuth, context: Context) {
    Column(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.umd_gluten_free_logo),
            contentDescription = "",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(200.dp)
        )
        Text("Would you like to logout?", modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(5.dp))
        Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Button(
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFe21833)),
                onClick = {
                    auth.signOut()
                    Toast.makeText(context, "Logout Successful!", Toast.LENGTH_SHORT).show()
                    onNavigateToMap()
                }

            ) {
                Text("Yes", color = Color.White)
            }
            Spacer(modifier = Modifier.width(15.dp))
            Button(
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFe21833)),
                onClick = {
                    onNavigateToMap()
                }

            ) {
                Text("No", color = Color.White)
            }

        }
    }
}


@Composable
fun TopBar(onMenuClicked: () -> Unit) {
    TopAppBar(
        title = {Text(text= "UMD Gluten Free", color=Color.White)},
        navigationIcon = {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",

                // When clicked trigger onClick
                // Callback to trigger drawer open
                modifier = Modifier.clickable(onClick = onMenuClicked),
                tint = Color.White
            )
            // A button to move map to user's location
        },
        backgroundColor = Color(0xFFe21833)
    )
}

@Composable
fun Drawer(
    onNavigateToAcctManagement: () -> Unit,
    onNavigateToSubmit: () -> Unit,
    auth: FirebaseAuth,
    filter: MutableState<Boolean>,
    filterGreaterThan: MutableState<Float>
) {

    Column(
        Modifier.fillMaxSize()
    ) {
        //first we want a logo or something so we can put the buttons closer to the
        // user's fingers
        val alignToCenter = Modifier.align(Alignment.CenterHorizontally)
        Image(
            painter = painterResource(id = R.drawable.umd_gluten_free_logo),
            contentDescription = "",
            modifier = alignToCenter.size(150.dp)
        )
        TextButton(
            onClick = onNavigateToSubmit,
            modifier = alignToCenter.fillMaxWidth()
        ) {Text(text = "\uD83C\uDF73 Submit New Meal \uD83E\uDD69",
            color=Color.Black,
            fontSize = 20.sp,
            textAlign = TextAlign.Center)}
        TextButton(
            onClick = onNavigateToAcctManagement,
            modifier = alignToCenter.fillMaxWidth()
            ) {Text( if (auth.currentUser != null) "\uD83D\uDC64 Log Out \uD83D\uDC64" else "\uD83D\uDC64 Log In \uD83D\uDC64" , color=Color.Black, fontSize = 17.sp)}
        Spacer(modifier = Modifier.height(125.dp))
        Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(text="Filter?", modifier = Modifier.align(Alignment.CenterVertically))
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = filter.value,
                onCheckedChange = {filter.value = it},
                modifier = Modifier.align(Alignment.CenterVertically),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFe21833),
                    checkedTrackColor = Color(0xFFda808c)
                )
            )



        }

        Text(text = "Show only results with a rating greater than: ", modifier = Modifier.align(Alignment.CenterHorizontally))
        Slider(
            value = filterGreaterThan.value,
            onValueChange = {filterGreaterThan.value = it},
            valueRange = 0f..5f,
            steps = 4,
            colors = SliderDefaults.colors(
                thumbColor = Color.Red,
                activeTrackColor = Color.Red
            ),
            modifier = Modifier.padding(60.dp, 10.dp)
        )
    }
}

@Composable
fun DrawerBody(
    db: DatabaseReference,
    toastContext: Context,
    filter: MutableState<Boolean>,
    filterGreaterThan: MutableState<Float>
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
    ) {
        ListScreen(db = db, toastContext = toastContext, filter = filter, filterGreaterThan = filterGreaterThan)
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun homeScreen(
    onNavigateToAcctManagement: () -> Unit,
    onNavigateToSubmit: () -> Unit,
    auth: FirebaseAuth,
    db: DatabaseReference,
    toastContext: Context,
    filter: MutableState<Boolean>,
    filterGreaterThan: MutableState<Float>
) {
    // to set menu closed by default
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val coroutineScope = rememberCoroutineScope()
    // Scaffold Composable
    Scaffold(

        // pass the scaffold state
        scaffoldState = scaffoldState,
        drawerGesturesEnabled = scaffoldState.drawerState.isOpen,
        // pass the topbar we created
        topBar = {
            TopBar(
                // When menu is clicked open the
                // drawer in coroutine scope
                onMenuClicked = {
                    coroutineScope.launch {
                        scaffoldState.drawerState.open()
                    }
                })
        },


        // Pass the body in
        // content parameter

        // THIS COULD BE A PROBLEM >:O
        content = {
            DrawerBody(db = db, toastContext = toastContext, filter = filter, filterGreaterThan = filterGreaterThan)
        },

        // pass the drawer
        drawerContent = {
            Drawer(
                onNavigateToAcctManagement = onNavigateToAcctManagement,
                onNavigateToSubmit = onNavigateToSubmit,
                auth =  auth,
                filter = filter,
                filterGreaterThan = filterGreaterThan
            )
        }
    )
}

