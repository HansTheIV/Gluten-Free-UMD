package com.example.umd_gluten_free

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.umd_gluten_free.ui.theme.UMDGlutenFreeTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        setContent {
            UMDGlutenFreeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {

                    AppNavHost(auth = auth)


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
    startDestination: String = "mapScreen"
) {
    NavHost(modifier=modifier, navController=navController, startDestination=startDestination) {
        composable("settingsScreen") { AccountManagementScreen(navController = navController, auth = auth ) }
        composable("listScreen") { ListScreen() }
        composable("submitNewFood") { SubmitScreen(auth = auth) }
        composable("forgotPassword") { ForgotPasswordScreen(auth = auth) }
        composable("signupScreen") { SignupScreen(
                                                navController = navController,
                                                context = LocalContext.current,
                                                auth = auth
                                            )
                                        }
        composable("loginScreen") {
            if(auth.currentUser == null) {
                LoginScreen(
                    navController = navController,
                    context = LocalContext.current,
                    auth = auth
                )
            }
            else {
                //Screen to allow logout
                Toast.makeText(LocalContext.current, "You are already logged in. Would you like to log out?", Toast.LENGTH_LONG).show()
                LogoutScreen(
                         auth = auth ,
                        navController = navController
                )
            }
        }
        composable("mapScreen") {
            MapScreen(
                onNavigateToAcctManagement = {
                    navController.navigate("settingsScreen") {
                        popUpTo("mapScreen")
                    }
                },
                onNavigateToList = {
                    navController.navigate("listScreen")
                },
                onNavigateToSubmit = {
                    navController.navigate("submitNewFood")
                },
                onNavigateToMap = {
                    navController.navigate("mapScreen")
                }

            )
        }


    }
}
// Top level screens
@Composable
fun SubmitScreen(auth: FirebaseAuth) {
    fun submitMeal(mealName: String, locationName: String, vegetarian: Boolean, rating: Int) {

        //this might work!
    }
    if (auth.currentUser == null) {
        // Cannot submit if not logged in, show error screen
    }
    else {
        Column(modifier = Modifier.fillMaxSize()) {
            val centered = Modifier.align(Alignment.CenterHorizontally)
            val mealName = remember { mutableStateOf(TextFieldValue()) }
            val mealLocation = remember { mutableStateOf(TextFieldValue()) }
            val rating = remember { mutableStateOf(0) }
            val isVegan = remember { mutableStateOf(true) }
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
            Text("Vegan?", modifier = centered)
            Checkbox(
                checked = isVegan.value,
                onCheckedChange = { isVegan.value = it },
                modifier = centered
            )
            Spacer(modifier = Modifier.height(30.dp))
            Button(
                onClick = {
                    submitMeal(
                        mealName.value.toString(),
                        mealLocation.value.toString(),
                        isVegan.value,
                        rating.value
                    )
                },
                modifier = centered,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFe21833))
            ) {
                Text("Submit Meal", color = Color.White)
            }
        }
    }
}
@Composable
fun LogoutScreen(
    auth: FirebaseAuth,
    navController: NavHostController
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = {
                // erase current login
                auth.signOut()
                navController.navigate("mapScreen")
            }) {
            Text(text = "Log out?")

        }

    }
}
@Composable
fun ListScreen() {
    Placeholder(component = "List View")
}

@Composable
fun AccountManagementScreen(
    navController: NavHostController,
    auth: FirebaseAuth
) {
    if(auth.currentUser != null)
    {
        //allow user to change their password
        Column(modifier = Modifier.fillMaxSize()) {

        }
    }
    else {
        // if not logged in, redirect to the login screen
        navController.navigate("loginScreen")
    }
}

@Composable
fun SignupScreen(
    navController: NavHostController,
    context: Context,
    auth: FirebaseAuth
) {
    fun submitSignupAttempt(email: String, password: String, isVegan: Boolean) {
        fun onResult(exception: java.lang.Exception?) {
            Toast.makeText(context, exception?.message, Toast.LENGTH_LONG).show()
        }
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
            onResult(it.exception)
        }
        // user is now either signed in, or the sign up failed. If the signup failed, it should
        // display the error as a Toast
    }

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val email = remember { mutableStateOf(TextFieldValue()) }
        val password = remember { mutableStateOf(TextFieldValue()) }
        val vegan = remember { mutableStateOf(false) }
        Text(text = "Sign Up", style = TextStyle(fontSize = 40.sp))

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
        Text(modifier = Modifier.align(Alignment.CenterHorizontally), text = "Vegan?")
        Switch(modifier = Modifier.align(Alignment.CenterHorizontally), checked = vegan.value, onCheckedChange = {vegan.value = it})
        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier.padding(40.dp, 0.dp, 40.dp, 0.dp)) {
            Button(
                onClick = {
                            submitSignupAttempt(email.value.toString(), password.value.toString(), vegan.value)
                            if(auth.currentUser != null) {
                                //if signup is successful, they are logged in and so we route them back to map screen.
                                navController.navigate("mapScreen")
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
fun ForgotPasswordScreen(auth: FirebaseAuth) {
    Placeholder("forgot password screen")
}

//sub-screens
@Composable
fun LoginScreen(
    navController: NavHostController,
    context: Context,
    auth: FirebaseAuth
) {

    fun submitLoginAttempt(email: String, password: String) {
        fun onResult(exception: java.lang.Exception?) {
            Toast.makeText(context, exception?.message, Toast.LENGTH_LONG).show()
        }
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                onResult(it.exception)
            }
        // User should now be logged  in.
    }



    Box(modifier = Modifier.fillMaxSize()) {

        Button(onClick = { navController.navigate("signupScreen") },
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

        Text(text = "Login Or Sign Up", style = TextStyle(fontSize = 40.sp))

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
        }
        Button(
            onClick = {
                submitLoginAttempt(email.value.toString(), password.value.toString())
                if(auth.currentUser != null) {
                    //that means there is a user logged in
                    navController.navigate("mapScreen")
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

        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = {navController.navigate("forgotPassword")},
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)) {
            Text("Forgot Password?")
        }
    }
}
// building blocks
@Composable
fun Placeholder(component: String) {
    Text(text = "$component goes here")
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
    onNavigateToList: () -> Unit,
    onNavigateToSubmit: () -> Unit,
    onNavigateToMap: () -> Unit
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
        // This will get a few more buttons to open other screens
        //TODO increase font size, decrease padding
        TextButton(
            onClick = onNavigateToSubmit,
            modifier = alignToCenter.fillMaxWidth()
        ) {Text("Submit New Meal", color=Color.Black)}
        TextButton(
            onClick = onNavigateToMap,
            modifier = alignToCenter.fillMaxWidth()
        ) {Text("Map View", color=Color.Black)}
        TextButton(
            onClick = onNavigateToList,
            modifier = alignToCenter.fillMaxWidth()
        ) {Text("List View", color=Color.Black)}
        TextButton(
            onClick = onNavigateToAcctManagement,
            modifier = alignToCenter.fillMaxWidth()
            ) {Text("Settings", color=Color.Black)}
    }
}

@Composable
fun DrawerBody() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
    ) {
        GoogleMap(cameraPositionState = CameraPositionState(position = CameraPosition(LatLng(38.9779990, -76.9287295), 15f, 0f, 0f)))
    }
}

@Composable
fun MapScreen(
    onNavigateToAcctManagement: () -> Unit,
    onNavigateToList: () -> Unit,
    onNavigateToSubmit: () -> Unit,
    onNavigateToMap: () -> Unit
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
        content = {
            DrawerBody()
        },

        // pass the drawer
        drawerContent = {
            Drawer(
                onNavigateToAcctManagement = onNavigateToAcctManagement,
                onNavigateToList = onNavigateToList,
                onNavigateToSubmit = onNavigateToSubmit,
                onNavigateToMap = onNavigateToMap
            )
        }
    )
}

