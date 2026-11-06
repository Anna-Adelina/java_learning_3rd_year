package com.booktracker.booktracker.controller;

import com.booktracker.booktracker.dto.AuthorDto;
import com.booktracker.booktracker.dto.BookDto;
import com.booktracker.booktracker.entity.ReadingStatus;
import com.booktracker.booktracker.service.AuthorService;
import com.booktracker.booktracker.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookWebController {

    private final BookService bookService;
    private final AuthorService authorService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("books", bookService.findAll());
        model.addAttribute("authors", authorService.findAll());
        model.addAttribute("statuses", ReadingStatus.values());
        return "books";
    }

    @PostMapping
    public String create(@ModelAttribute BookDto bookDto) {
        bookService.create(bookDto);
        return "redirect:/books";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        bookService.delete(id);
        return "redirect:/books";
    }
}