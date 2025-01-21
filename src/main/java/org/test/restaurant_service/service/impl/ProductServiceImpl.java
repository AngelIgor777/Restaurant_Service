package org.test.restaurant_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.test.restaurant_service.dto.request.ProductRequestDTO;
import org.test.restaurant_service.dto.response.PhotoResponseDTO;
import org.test.restaurant_service.dto.response.ProductAndPhotosResponseDTO;
import org.test.restaurant_service.dto.response.ProductResponseDTO;
import org.test.restaurant_service.entity.Photo;
import org.test.restaurant_service.entity.Product;
import org.test.restaurant_service.entity.ProductType;
import org.test.restaurant_service.mapper.PhotoMapper;
import org.test.restaurant_service.mapper.ProductMapper;
import org.test.restaurant_service.repository.PhotoRepository;
import org.test.restaurant_service.repository.ProductHistoryRepository;
import org.test.restaurant_service.repository.ProductRepository;
import org.test.restaurant_service.repository.ProductTypeRepository;
import org.test.restaurant_service.service.ProductService;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductTypeRepository productTypeRepository;
    private final ProductMapper productMapper;
    private final PhotoMapper photoMapper;
    private final PhotoRepository photoRepository;
    private final PhotoServiceImpl photoServiceImpl;

    @Override
    public Product parseRequest(String name, String description, Integer typeId, BigDecimal price, String cookingTime) {
        ProductType type = productTypeRepository.findById(typeId)
                .orElseThrow(() -> new EntityNotFoundException("ProductType not found with id :" + typeId));
        Product product = new Product();
        product.setName(name);
        product.setType(type);
        product.setDescription(description);
        product.setPrice(price);
        product.setCookingTime(LocalTime.parse(cookingTime));
        return product;
    }

    public Product create(Product product, Integer typeId) {
        ProductType type = productTypeRepository.findById(typeId)
                .orElseThrow(() -> new EntityNotFoundException("ProductType not found with id :" + typeId));
        product.setType(type);
        product = productRepository.save(product);

        return product;
    }


    @Override
    public Product create(ProductRequestDTO productRequestDTO) {
        Product product = productMapper.toEntity(productRequestDTO);
        ProductType type = productTypeRepository.findById(productRequestDTO.getTypeId())
                .orElseThrow(() -> new EntityNotFoundException("ProductType not found with id :" + productRequestDTO.getTypeId()));

        product.setType(type);
        return productRepository.save(product);
    }

    @Override
    public Product update(Product updatedProduct, Integer id) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + id));

        // Update only non-null fields in the updatedProduct
        if (updatedProduct.getName() != null) {
            existingProduct.setName(updatedProduct.getName());
        }

        if (updatedProduct.getDescription() != null) {
            existingProduct.setDescription(updatedProduct.getDescription());
        }

        if (updatedProduct.getType() != null) {
            existingProduct.setType(updatedProduct.getType());
        }

        if (updatedProduct.getPrice() != null) {
            existingProduct.setPrice(updatedProduct.getPrice());
        }

        if (updatedProduct.getCookingTime() != null) {
            existingProduct.setCookingTime(updatedProduct.getCookingTime());
        }

        return productRepository.save(existingProduct);
    }

    @Override
    public void delete(Integer id) {
        List<Photo> photoList = photoRepository.findAllByProductId(id);
        productRepository.deleteById(id);
        photoRepository.deleteAll(photoList);

        for (Photo photo : photoList) {
            photoServiceImpl.deleteImage(Objects.requireNonNull(photo.getImage().getOriginalFilename()));
        }
    }


    @Override
    public ProductAndPhotosResponseDTO getById(Integer id) {
        Product product = productRepository.findByIdWithPhotos(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id " + id));
        List<PhotoResponseDTO> photoResponseDTOS = product.getPhotos()
                .stream()
                .map(photoMapper::toResponseDTO).toList();

        ProductResponseDTO productResponseDTO = productMapper.toResponseDTO(product);
        ProductAndPhotosResponseDTO productAndPhotosResponseDTO = new ProductAndPhotosResponseDTO(productResponseDTO, photoResponseDTOS);
        return productAndPhotosResponseDTO;
    }

    @Override
    public Page<ProductResponseDTO> getAll(Integer typeId, Pageable pageable) {
        Page<Product> products;
        if (typeId != null) {
            products = productRepository.findAllByTypeId(typeId, pageable);
        } else {
            products = productRepository.findAll(pageable);
        }
        return products.map(productMapper::toResponseDTO);
    }

    @Cacheable(value = "product", key = "#productName")
    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getByName(String productName) {
        Product product = productRepository.findByName(productName)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with name " + productName));
        return productMapper.toResponseDTO(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getByTypeName(String typeName) {
        List<Product> allByTypeName = productRepository.findAllByType_Name(typeName);
        return allByTypeName.stream().map(productMapper::toResponseDTO).toList();
    }
}
