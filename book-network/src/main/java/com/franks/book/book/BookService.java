package com.franks.book.book;

import com.franks.book.common.PageResponse;
import com.franks.book.exception.OperationNotPermittedException;
import com.franks.book.file.FileStorageService;
import com.franks.book.history.BookTransactionHistory;
import com.franks.book.history.BookTransactionHistoryRepository;
import com.franks.book.notification.Notification;
import com.franks.book.notification.NotificationService;
import com.franks.book.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.naming.OperationNotSupportedException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.franks.book.book.BookSpecification.withOwnerId;
import static com.franks.book.notification.NotificationStatus.*;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final BookTransactionHistoryRepository bookTransactionHistoryRepository;
    private final BookMapper bookMapper;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    public Integer save(BookRequest request, Authentication connectedUser) {
        //User user = ((User) connectedUser.getPrincipal());
        Book book = bookMapper.toBook(request);
        //book.setOwner(user);
        return bookRepository.save(book).getId();
    }

    public BookResponse findById(Integer bookId) {
        return bookRepository.findById(bookId)
                .map(bookMapper::toBookResponse)
                .orElseThrow(()-> new EntityNotFoundException("No book found with the ID"));
    }

    public PageResponse<BookResponse> findAllBooks(int page, int size, Authentication connectedUser) {
        //User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page,size, Sort.by("createdDate").descending());
        Page<Book> books = bookRepository.findAllDisplayableBooks(pageable,connectedUser.getName());
        List<BookResponse> bookResponse = books.stream()
                .map(bookMapper:: toBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponse,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BookResponse> findAllBooksByOwner(int page, int size, Authentication connectedUser) {
        //User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page,size, Sort.by("createdDate").descending());
        Page<Book> books = bookRepository.findAll(withOwnerId(connectedUser.getName()),pageable);
        List<BookResponse> bookResponse = books.stream()
                .map(bookMapper:: toBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponse,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllBorrowedBooks(int page, int size, Authentication connectedUser) {
        //User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page,size, Sort.by("createdDate").descending());
        Page<BookTransactionHistory> allBorrowedBooks = bookTransactionHistoryRepository.findAllBorrowebBooks(pageable, connectedUser.getName());
        List<BorrowedBookResponse> bookResponse = allBorrowedBooks.stream()
                .map(bookMapper::toBorrowebBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponse,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllReturnedBooks(int page, int size, Authentication connectedUser) {
        //User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page,size, Sort.by("createdDate").descending());
        Page<BookTransactionHistory> allBorrowedBooks = bookTransactionHistoryRepository.findAllReturnedBooks(pageable, connectedUser.getName());
        List<BorrowedBookResponse> bookResponse = allBorrowedBooks.stream()
                .map(bookMapper::toBorrowebBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponse,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
    }

    public Integer updateShareableStatus(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(()-> new EntityNotFoundException("No book found with ID:: "+ bookId));
       //User user = ((User) connectedUser.getPrincipal());
        if(!Objects.equals(book.getCreatedBy(),connectedUser.getName())){
            throw new OperationNotPermittedException("You cannot update others shareable status");
        }
        book.setShareable(!book.isShareable());
        bookRepository.save(book);
        return bookId;
    }

    public Integer updateArchivedStatus(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(()-> new EntityNotFoundException("No book found with ID:: "+ bookId));
        //User user = ((User) connectedUser.getPrincipal());
        if(!Objects.equals(book.getCreatedBy(),connectedUser.getName())){
            throw new OperationNotPermittedException("You cannot update others archived status");
        }
        book.setArchived(!book.isArchived());
        bookRepository.save(book);
        return bookId;
    }

    public Integer borrowBook(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No Book found with the ID::" + bookId));
        if(book.isArchived()|| !book.isShareable()){
            throw new OperationNotPermittedException("The requested book cannot be borrowed since it is archived or not shareable");

        }
        //User user = ((User) connectedUser.getPrincipal());
        if(Objects.equals(book.getCreatedBy(),connectedUser.getName())){
            throw new OperationNotPermittedException("You cannot borrow your own book");
        }

        final boolean isAlreadyBorrowed= bookTransactionHistoryRepository.isAlreadyBorrowedByUser(bookId,connectedUser.getName());
        if(isAlreadyBorrowed){
            throw new OperationNotPermittedException("The requested book is already borrowed");

        }
        BookTransactionHistory bookTransactionHistory = BookTransactionHistory.builder()
                .userId(connectedUser.getName())
                .book(book)
                .returned(false)
                .returnApproved(false)
                .build();
        notificationService.sendNotification(
                book.getCreatedBy(),
                Notification.builder()
                        .status(BORROWED)
                        .message("Your book has been borrowed")
                        .bookTitle(book.getTitle())
                        .build()
        );
        return bookTransactionHistoryRepository.save(bookTransactionHistory).getId();
    }

    public Integer returnBorrowedBook(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No Book found with the ID::" + bookId));
        if(book.isArchived()|| !book.isShareable()){
            throw new OperationNotPermittedException("The requested book cannot be borrowed since it is archived or not shareable");

        }
        //User user = ((User) connectedUser.getPrincipal());
        if(Objects.equals(book.getCreatedBy(),connectedUser.getName())){
            throw new OperationNotPermittedException("You cannot borrow or return your own book");
        }
        BookTransactionHistory bookTransactionHistory = bookTransactionHistoryRepository.findByBookIdAndUserId(bookId, connectedUser.getName())
                .orElseThrow( () -> new OperationNotPermittedException("You did not borrow this book"));
        bookTransactionHistory.setReturned(true);

        var saved = bookTransactionHistoryRepository.save(bookTransactionHistory);
        notificationService.sendNotification(
                book.getCreatedBy(),
                Notification.builder()
                        .status(RETURNED)
                        .message("Your book has been returned")
                        .bookTitle(book.getTitle())
                        .build()
        );
        return saved.getId();

    }

    public Integer approveReturnBorrowedBook(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No Book found with the ID::" + bookId));
        if(book.isArchived()|| !book.isShareable()){
            throw new OperationNotPermittedException("The requested book cannot be borrowed since it is archived or not shareable");

        }
        //User user = ((User) connectedUser.getPrincipal());
        if(!Objects.equals(book.getCreatedBy(),connectedUser.getName())){
            throw new OperationNotPermittedException("You cannot borrow or return your own book");
        }
        BookTransactionHistory bookTransactionHistory = bookTransactionHistoryRepository.findByBookIdAndOwnerId(bookId, connectedUser.getName())
                .orElseThrow( () -> new OperationNotPermittedException("The book is not returned yet.You cannot approve its return"));
        bookTransactionHistory.setReturnApproved(true);

        var saved = bookTransactionHistoryRepository.save(bookTransactionHistory);
        notificationService.sendNotification(
                bookTransactionHistory.getCreatedBy(),
                Notification.builder()
                        .status(RETURN_APPROVED)
                        .message("Your book return has been approved")
                        .bookTitle(book.getTitle())
                        .build()
        );
        return saved.getId();
    }

    public void uploadBookCoverPicture(MultipartFile file, Authentication connectedUser, Integer bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No Book found with the ID::" + bookId));
        //User user = ((User) connectedUser.getPrincipal());
        var bookCover = fileStorageService.saveFile(file,connectedUser.getName());
        book.setBookCover(bookCover);
        bookRepository.save(book);
    }
}
