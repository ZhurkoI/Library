package org.zhurko.library.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.zhurko.library.model.Label;
import org.zhurko.library.model.Post;
import org.zhurko.library.model.PostStatus;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonPostRepositoryImpl implements PostRepository {

    private static final String FILE_PATH = "src/main/resources/posts.json";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public Post getById(Long id) {
        return getAllPostsInternal().stream()
                .filter(p -> p.getId().equals(id))
                .filter(p -> p.getPostStatus() != PostStatus.DELETED)
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Post> getAll() {
        return getAllPostsInternal()
                .stream()
                .filter(p -> p.getPostStatus() != PostStatus.DELETED)
                .collect(Collectors.toList());
    }

    @Override
    public Post save(Post post) {
        List<Post> posts = getAllPostsInternal();
        post.setId(generateId(posts));
        post.setPostStatus(PostStatus.UNDER_REVIEW);
        Date date = new Date();
        post.setCreated(date);
        post.setUpdated(date);
        List<Post> modifiableListOfPosts = new ArrayList<>(posts);
        modifiableListOfPosts.add(post);
        writeToFile(modifiableListOfPosts);
        return post;
    }

    @Override
    public Post update(Post post) {
        post.setUpdated(new Date());
        post.setPostStatus(PostStatus.ACTIVE);
        List<Post> posts = getAllPostsInternal();
        posts.forEach(p -> {
            if (p.getId().equals(post.getId())) {
                p.setContent(post.getContent());
                p.setUpdated(post.getUpdated());
                p.setPostStatus(post.getPostStatus());
            }
        });
        writeToFile(posts);
        return post;
    }

    @Override
    public void deleteById(Long id) {
        List<Post> posts = getAllPostsInternal();
        posts.forEach(p -> {
            if (p.getId().equals(id)) {
                p.setPostStatus(PostStatus.DELETED);
                p.setUpdated(new Date());
            }
        });
        writeToFile(posts);
    }

    private List<Post> getAllPostsInternal() {
        List<Post> posts;

        Type listType = new TypeToken<ArrayList<Post>>() {
        }.getType();

        try (FileReader reader = new FileReader(FILE_PATH)) {
            posts = this.gson.fromJson(reader, listType);
        } catch (IOException exception) {
            exception.printStackTrace();
            return Collections.emptyList();
        }

        if (posts == null) {
            return Collections.emptyList();
        } else {
            return posts;
        }
    }

    private Long generateId(List<Post> posts) {
        long maxIndex;
        Optional<Post> mostRecentEntry = posts.stream().max(Comparator.comparingLong(Post::getId));
        if (mostRecentEntry.isPresent()) {
            maxIndex = mostRecentEntry.get().getId() + 1L;
        } else {
            maxIndex = 1L;
        }
        return maxIndex;
    }

    private void writeToFile(List<Post> posts) {
        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            this.gson.toJson(posts, writer);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public Post addLabel(Post post, Label label) {
        post.addLabel(label);
        List<Post> posts = getAllPostsInternal();
        posts.forEach(p -> {
            if (p.getId().equals(post.getId())) {
                p.setAllLabels(post.getLabels());
            }
        });
        writeToFile(posts);
        return post;
    }

    @Override
    public Post removeLabel(Post post, Label label) {
        List<Post> posts = getAllPostsInternal();
        Set<Label> labels = post.getLabels();
        labels.remove(label);
        posts.forEach(p -> {
            if (p.getId().equals(post.getId())) {
                p.setAllLabels(labels);
            }
        });
        writeToFile(posts);
        return post;
    }
}
