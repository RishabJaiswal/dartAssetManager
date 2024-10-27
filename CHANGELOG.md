<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Flutter Asset Viewer Changelog

## [Unreleased]
### Added
- Future features will be listed here

## [0.2.1] - 2024-10-29
### Changed
- Set min IDE supported version to 231

## [0.2.0] - 2024-10-28
### Added
- Image preview functionality
    - Click any image to open in IDE viewer
    - Support for PNG, JPG, SVG formats
    - Native IDE viewer integration
- Real-time search with 500ms debounce
    - Instant filtering as you type
    - Efficient performance with large image sets
- Dynamic loading indicators
- Theme-aware UI components

### Changed
- Enhanced UI/UX
    - Added hover effects for better interaction
    - Improved list item spacing
    - 60x60 optimized image thumbnails
- Image loading performance optimizations
    - Batch loading implementation
    - Efficient memory usage
    - Better error handling

### Fixed
- Initial package loading now shows images automatically
- Search loading indicator cleanup
- List item height consistency
- Image scaling and aspect ratio handling

## [0.1.0] - 2024-10-27
### Added
- Initial plugin release
- Basic Features:
    - Flutter package asset viewing
    - Image list with thumbnails
    - Package selection dropdown
    - Basic search functionality
    - Support for multiple image formats
- Project Features:
    - Package navigation
    - Image file path display
    - Relative path computation
    - Basic error handling

### Security
- Proper file access handli