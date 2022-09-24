package dev.kason.eightlakes

import java.awt.Color
import javax.swing.*

// pretty spicy colors
fun main() {
    val frame = JFrame("Eight Lakes")
    val panel = JPanel()
    // grid of 10x10 buttons
    val grid = Array(10) {
        Array(10) {
            val button = JButton()
            // set the button color to generateRandomColor()
            button.background = Color(generateRandomColor().rgb)
            button
        }
    }
    // add buttons to panel
    grid.forEach { row ->
        row.forEach { button ->
            panel.add(button)
        }
    }
    frame.add(panel)
    frame.pack()
    frame.isVisible = true
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
}