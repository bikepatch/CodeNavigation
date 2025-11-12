import com.example.search.searchForTextOccurrences
import kotlinx.coroutines.runBlocking
import java.nio.file.Path

fun main() = runBlocking {
    val dir = Path.of("C:/Users/User/IdeaProjects/CodeNavigation/Occurence")
    val searchString = "Occurence"

    println("Searching for \"$searchString\" under: $dir")

    searchForTextOccurrences(searchString, dir).collect { occ ->
        println("Found in file=${occ.file.fileName} line=${occ.line} offset=${occ.offset}")
    }

    println("Done.")
}