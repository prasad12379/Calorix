data class UserResponse(
    val message: String,
    val data: UserData
)

data class UserData(
    val name: String,
    val email: String,
    val age: Int,
    val gender: String,
    val height: Int,
    val weight: Int,
    val created_at: String
)
